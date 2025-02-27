package org.thoughtcrime.securesms.stories.tabs

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import org.signal.core.util.DimensionUnit
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.visible
import java.text.NumberFormat

/**
 * Displays the "Chats" and "Stories" tab to a user.
 */
class ConversationListTabsFragment : Fragment(R.layout.conversation_list_tabs) {

  private val viewModel: ConversationListTabsViewModel by viewModels(ownerProducer = { requireActivity() })

  private lateinit var chatsUnreadIndicator: TextView
  private lateinit var storiesUnreadIndicator: TextView
  private lateinit var chatsIcon: LottieAnimationView
  private lateinit var storiesIcon: LottieAnimationView
  private lateinit var chatsPill: ImageView
  private lateinit var storiesPill: ImageView

  private var pillAnimator: Animator? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    chatsUnreadIndicator = view.findViewById(R.id.chats_unread_indicator)
    storiesUnreadIndicator = view.findViewById(R.id.stories_unread_indicator)
    chatsIcon = view.findViewById(R.id.chats_tab_icon)
    storiesIcon = view.findViewById(R.id.stories_tab_icon)
    chatsPill = view.findViewById(R.id.chats_pill)
    storiesPill = view.findViewById(R.id.stories_pill)

    val iconTint = ContextCompat.getColor(requireContext(), R.color.signal_colorOnSecondaryContainer)

    chatsIcon.addValueCallback(
      KeyPath("**"),
      LottieProperty.COLOR
    ) { iconTint }

    storiesIcon.addValueCallback(
      KeyPath("**"),
      LottieProperty.COLOR
    ) { iconTint }

    view.findViewById<View>(R.id.chats_tab_touch_point).setOnClickListener {
      viewModel.onChatsSelected()
    }

    view.findViewById<View>(R.id.stories_tab_touch_point).setOnClickListener {
      viewModel.onStoriesSelected()
    }

    viewModel.state.observe(viewLifecycleOwner, this::update)
  }

  private fun update(state: ConversationListTabsState) {
    val wasChatSelected = chatsIcon.isSelected

    chatsIcon.isSelected = state.tab == ConversationListTab.CHATS
    storiesIcon.isSelected = state.tab == ConversationListTab.STORIES

    chatsPill.isSelected = chatsIcon.isSelected
    storiesPill.isSelected = storiesIcon.isSelected

    if (chatsIcon.isSelected xor wasChatSelected) {
      runLottieAnimations(chatsIcon, storiesIcon)
      runPillAnimation(chatsPill, storiesPill)
    }

    chatsUnreadIndicator.visible = state.unreadChatsCount > 0
    chatsUnreadIndicator.text = formatCount(state.unreadChatsCount)

    storiesUnreadIndicator.visible = state.unreadStoriesCount > 0
    storiesUnreadIndicator.text = formatCount(state.unreadStoriesCount)

    requireView().visible = !state.isSearchOpen
  }

  private fun runLottieAnimations(vararg toAnimate: LottieAnimationView) {
    toAnimate.forEach {
      if (it.isSelected) {
        it.resumeAnimation()
      } else {
        if (it.isAnimating) {
          it.pauseAnimation()
        }

        it.progress = 0f
      }
    }
  }

  private fun runPillAnimation(vararg toAnimate: ImageView) {
    val (selected, unselected) = toAnimate.partition { it.isSelected }

    pillAnimator?.cancel()
    pillAnimator = AnimatorSet().apply {
      duration = 150
      interpolator = PathInterpolatorCompat.create(0.17f, 0.17f, 0f, 1f)
      playTogether(
        selected.map { view ->
          view.visibility = View.VISIBLE
          ValueAnimator.ofInt(view.paddingLeft, 0).apply {
            addUpdateListener {
              view.setPadding(it.animatedValue as Int, 0, it.animatedValue as Int, 0)
            }
          }
        }
      )
      start()
    }

    unselected.forEach {
      val smallPad = DimensionUnit.DP.toPixels(16f).toInt()
      it.setPadding(smallPad, 0, smallPad, 0)
      it.visibility = View.INVISIBLE
    }
  }

  private fun formatCount(count: Long): String {
    if (count > 99L) {
      return getString(R.string.ConversationListTabs__99p)
    }
    return NumberFormat.getInstance().format(count)
  }
}
