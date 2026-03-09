package net.poopyfeed.pf.sharing

import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharingAdapterTest {

  private val themedContext
    get() =
        android.view.ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), net.poopyfeed.pf.R.style.Theme_Pf)

  private class DummyCallbacks : SharingInviteCallbacks {
    var copied: Int? = null
    var toggled: Int? = null
    var deleted: Int? = null

    override fun onCopyLink(invite: net.poopyfeed.pf.data.models.ChildInvite) {
      copied = invite.id
    }

    override fun onToggleInvite(invite: net.poopyfeed.pf.data.models.ChildInvite) {
      toggled = invite.id
    }

    override fun onDeleteInvite(invite: net.poopyfeed.pf.data.models.ChildInvite) {
      deleted = invite.id
    }
  }

  @Test
  fun headerItem_bindsTitleText() {
    val adapter = SharingAdapter(DummyCallbacks())
    val parent = android.widget.FrameLayout(themedContext)
    adapter.submitList(listOf(SharingListItem.InviteLinkHeader(title = "Invite links")))

    val viewHolder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
    adapter.onBindViewHolder(viewHolder, 0)

    val titleView =
        viewHolder.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_section_title)
    assertNotNull(titleView)
    assertEquals("Invite links", titleView.text.toString())
  }

  @Test
  fun shareRow_formatsRoleDisplayForKnownRoles() {
    val adapter = SharingAdapter(DummyCallbacks())
    val parent = android.widget.FrameLayout(themedContext)
    val shareCoParent =
        net.poopyfeed.pf.data.models.ChildShare(
            id = 1,
            userEmail = "user@example.com",
            role = "co-parent",
            roleDisplay = "Co-parent",
            createdAt = "",
        )
    val shareCaregiver =
        net.poopyfeed.pf.data.models.ChildShare(
            id = 2,
            userEmail = "care@example.com",
            role = "caregiver",
            roleDisplay = "Caregiver",
            createdAt = "",
        )
    adapter.submitList(
        listOf(
            SharingListItem.ShareRow(shareCoParent),
            SharingListItem.ShareRow(shareCaregiver),
        ))

    val vh0 = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
    adapter.onBindViewHolder(vh0, 0)
    val role0 =
        vh0.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_role)?.text?.toString()
    assertEquals(
        themedContext.getString(net.poopyfeed.pf.R.string.sharing_role_co_parent), role0)

    val vh1 = adapter.onCreateViewHolder(parent, adapter.getItemViewType(1))
    adapter.onBindViewHolder(vh1, 1)
    val role1 =
        vh1.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_role)?.text?.toString()
    assertEquals(
        themedContext.getString(net.poopyfeed.pf.R.string.sharing_role_caregiver), role1)
  }

  @Test
  fun inviteRow_setsPausedChipVisibilityAndToggleLabel() {
    val callbacks = DummyCallbacks()
    val adapter = SharingAdapter(callbacks)
    val parent = android.widget.FrameLayout(themedContext)
    val activeInvite =
        net.poopyfeed.pf.data.models.ChildInvite(
            id = 1,
            token = "t1",
            role = "co-parent",
            roleDisplay = "Co-parent",
            isActive = true,
            createdAt = "2024-01-01",
            inviteUrl = null,
        )
    val pausedInvite = activeInvite.copy(id = 2, isActive = false)
    adapter.submitList(
        listOf(SharingListItem.InviteRow(activeInvite), SharingListItem.InviteRow(pausedInvite)))

    val vhActive = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
    adapter.onBindViewHolder(vhActive, 0)
    val chipActive =
        vhActive.itemView.findViewById<android.view.View>(
            net.poopyfeed.pf.R.id.chip_paused)
    val toggleActive =
        vhActive.itemView.findViewById<android.widget.Button>(
            net.poopyfeed.pf.R.id.button_toggle)
    assertEquals(android.view.View.GONE, chipActive.visibility)
    assertEquals(
        themedContext.getString(net.poopyfeed.pf.R.string.invite_link_pause),
        toggleActive.text.toString())

    val vhPaused = adapter.onCreateViewHolder(parent, adapter.getItemViewType(1))
    adapter.onBindViewHolder(vhPaused, 1)
    val chipPaused =
        vhPaused.itemView.findViewById<android.view.View>(
            net.poopyfeed.pf.R.id.chip_paused)
    val togglePaused =
        vhPaused.itemView.findViewById<android.widget.Button>(
            net.poopyfeed.pf.R.id.button_toggle)
    assertEquals(android.view.View.VISIBLE, chipPaused.visibility)
    assertEquals(
        themedContext.getString(net.poopyfeed.pf.R.string.invite_link_resume),
        togglePaused.text.toString())
  }
}
