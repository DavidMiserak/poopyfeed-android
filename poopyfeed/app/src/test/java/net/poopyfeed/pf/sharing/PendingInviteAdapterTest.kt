package net.poopyfeed.pf.sharing

import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingInviteAdapterTest {

  private val themedContext
    get() =
        android.view.ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), net.poopyfeed.pf.R.style.Theme_Pf)

  @Test
  fun bind_withChildName_usesChildTitleString() {
    val adapter = PendingInviteAdapter { _, _ -> }
    val parent = android.widget.FrameLayout(themedContext)
    val invite =
        net.poopyfeed.pf.data.models.ShareInvite(
            id = 1,
            child = 5,
            token = "abc",
            role = "co-parent",
            isActive = true,
            createdAt = "",
            inviteUrl = null,
        )
    adapter.submitList(listOf(ShareInviteWithChildName(invite = invite, childName = "Baby Sam")))

    val viewHolder = adapter.onCreateViewHolder(parent, 0)
    adapter.onBindViewHolder(viewHolder, 0)

    val titleView =
        viewHolder.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_title)
    assertNotNull(titleView)
    val expected =
        themedContext.getString(net.poopyfeed.pf.R.string.pending_invite_for_child, "Baby Sam")
    assertEquals(expected, titleView.text.toString())
  }

  @Test
  fun bind_withoutChildName_fallsBackToRoleTitleStringAndFormatsRole() {
    val adapter = PendingInviteAdapter { _, _ -> }
    val parent = android.widget.FrameLayout(themedContext)
    val invite =
        net.poopyfeed.pf.data.models.ShareInvite(
            id = 2,
            child = 6,
            token = "xyz",
            role = "caregiver",
            isActive = true,
            createdAt = "",
            inviteUrl = null,
        )
    adapter.submitList(listOf(ShareInviteWithChildName(invite = invite, childName = null)))

    val viewHolder = adapter.onCreateViewHolder(parent, 0)
    adapter.onBindViewHolder(viewHolder, 0)

    val titleView =
        viewHolder.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_title)
    val roleView =
        viewHolder.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_role)
    assertNotNull(titleView)
    assertNotNull(roleView)

    val expectedTitle =
        themedContext.getString(net.poopyfeed.pf.R.string.pending_invite_for_role, "caregiver")
    assertEquals(expectedTitle, titleView.text.toString())

    val expectedRole =
        themedContext.getString(net.poopyfeed.pf.R.string.sharing_role_caregiver)
    assertEquals(expectedRole, roleView.text.toString())
  }
}
