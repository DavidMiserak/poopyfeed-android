package net.poopyfeed.pf.sharing

import net.poopyfeed.pf.data.models.ChildInvite
import net.poopyfeed.pf.data.models.ChildShare

/** Item for the sharing screen list: section headers and rows. */
sealed class SharingListItem {

  data class InviteLinkHeader(val title: String) : SharingListItem()

  data class InviteRow(val invite: ChildInvite) : SharingListItem()

  data class PeopleHeader(val title: String) : SharingListItem()

  data class ShareRow(val share: ChildShare) : SharingListItem()
}
