/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.fragments.player.styles.peekplayerstyle

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentPeekPlayerBinding
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.fragments.player.*
import com.mardous.booming.fragments.player.base.AbsPlayerControlsFragment
import com.mardous.booming.fragments.player.base.AbsPlayerFragment
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class PeekPlayerFragment : AbsPlayerFragment(R.layout.fragment_peek_player) {

    private var _binding: FragmentPeekPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: PeekPlayerControlsFragment

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.Peek)

    override val playerToolbar: Toolbar
        get() = binding.playerToolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPeekPlayerBinding.bind(view)
        setupToolbar()
        inflateMenuInView(playerToolbar)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val navigationBar = insets.getInsets(Type.systemBars())
            v.updatePadding(bottom = navigationBar.bottom)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            insets
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect { song ->
                _binding?.let { nonNullBinding ->
                    nonNullBinding.title.text = song.title
                    nonNullBinding.text.text = getSongArtist(song)
                }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.extraInfoFlow.collect {
                _binding?.let { nonNullBinding ->
                    if (isExtraInfoEnabled()) {
                        nonNullBinding.songInfo.text = it
                        nonNullBinding.songInfo.isVisible = true
                    } else {
                        nonNullBinding.songInfo.isVisible = false
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        playerToolbar.setNavigationOnClickListener {
            getOnBackPressedDispatcher().onBackPressed()
        }
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.setShowAsAction(R.id.action_playing_queue)
        menu.setShowAsAction(R.id.action_favorite)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldPrimaryTextColor = binding.title.currentTextColor
        val oldSecondaryTextColor = binding.text.currentTextColor
        return mutableListOf(
            binding.root.surfaceTintTarget(scheme.surfaceColor),
            binding.title.tintTarget(oldPrimaryTextColor, scheme.primaryTextColor),
            binding.text.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor)
        ).also {
            it.addAll(playerControlsFragment.getTintTargets(scheme))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}