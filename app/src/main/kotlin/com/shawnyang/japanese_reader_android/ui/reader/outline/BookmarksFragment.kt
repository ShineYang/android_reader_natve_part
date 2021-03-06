package com.shawnyang.japanese_reader_android.ui.reader.outline

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.shawnyang.japanese_reader_android.R
import com.shawnyang.japanese_reader_android.data.Bookmark
import com.shawnyang.japanese_reader_android.data.db.BookData
import com.shawnyang.japanese_reader_android.ui.base.BaseLazyFragment
import com.shawnyang.japanese_reader_android.ui.reader.react.ReaderViewModel
import kotlinx.android.synthetic.main.item_recycle_bookmark.view.*
import kotlinx.android.synthetic.main.layout_sheet_content_listview.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.publication.Publication
import outlineTitle
import kotlin.math.roundToInt

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */

class BookmarksFragment : Fragment(R.layout.layout_bookmark_list) {
    lateinit var publication: Publication
    lateinit var persistence: BookData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            persistence = it.persistence
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val comparator: Comparator<Bookmark> = compareBy( {it.resourceIndex },{ it.location.progression })
        val bookmarks = persistence.getBookmarks(comparator).toMutableList()

        list_view.adapter = BookMarksAdapter(
            requireActivity(),
            bookmarks,
            publication,
            onBookmarkDeleteRequested = {
                persistence.removeBookmark(it.id!!)
            }
        )

        list_view.setOnItemClickListener { _, _, position, _ -> onBookmarkSelected(bookmarks[position]) }
    }

    private fun onBookmarkSelected(bookmark: Bookmark) {
        setFragmentResult(
            OutlineContract.REQUEST_KEY,
            OutlineContract.createResult(bookmark.locator)
        )
    }
}

private class BookMarksAdapter(
    private val activity: Activity,
    private val bookmarks: MutableList<Bookmark>,
    private val publication: Publication,
    private val onBookmarkDeleteRequested: (Bookmark) -> Unit
) : BaseAdapter() {

    private class ViewHolder(row: View) {
        val bookmarkChapter: TextView = row.bookmark_chapter
        val bookmarkProgression: TextView = row.bookmark_progression
        val bookmarkTimestamp: TextView = row.bookmark_timestamp
        val bookmarkOverflow: ImageView = row.overflow
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view =
            if (convertView == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                inflater.inflate(R.layout.item_recycle_bookmark, null).also {
                    it.tag = ViewHolder(it)
                }
            } else {
                convertView
            }

        val viewHolder = view.tag as ViewHolder

        val bookmark = getItem(position) as Bookmark

        val title = getBookSpineItem(bookmark.resourceHref)
            ?:  "*Title Missing*"

        viewHolder.bookmarkChapter.text = title

        bookmark.location.progression?.let { progression ->
            val formattedProgression = "${(progression * 100).roundToInt()}% through resource"
            viewHolder.bookmarkProgression.text = formattedProgression
        }

        val formattedDate = DateTime(bookmark.creationDate).toString(DateTimeFormat.shortDateTime())
        viewHolder.bookmarkTimestamp.text = formattedDate

        viewHolder.bookmarkOverflow.setOnClickListener {

            val popupMenu = PopupMenu(parent?.context, viewHolder.bookmarkChapter)
            popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
            popupMenu.show()

            popupMenu.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.delete) {
                    onBookmarkDeleteRequested(bookmarks[position])
                    bookmarks.removeAt(position)
                    notifyDataSetChanged()
                }
                false
            }
        }

        return view
    }

    override fun getCount(): Int {
        return bookmarks.size
    }

    override fun getItem(position: Int): Any {
        return bookmarks[position]
    }

    private fun getBookSpineItem(href: String): String? {
        for (link in publication.tableOfContents) {
            if (link.href == href) {
                return link.outlineTitle
            }
        }
        for (link in publication.readingOrder) {
            if (link.href == href) {
                return link.outlineTitle
            }
        }
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}