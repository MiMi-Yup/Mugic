/*
 * Copyright (c) 2019 Naman Dwivedi.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.uitk15.mugic.ui.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.uitk15.mugic.R
import com.uitk15.mugic.databinding.FragmentPlaylistsBinding
import com.uitk15.mugic.extensions.*
import com.uitk15.mugic.models.Playlist
import com.uitk15.mugic.ui.adapters.PlaylistAdapter
import com.uitk15.mugic.ui.dialogs.CreatePlaylistDialog
import com.uitk15.mugic.ui.fragments.base.MediaItemFragment
import com.uitk15.mugic.util.AutoClearedValue
import io.github.uditkarode.able.models.Song
import io.github.uditkarode.able.models.SongState
import io.github.uditkarode.able.services.MusicService
import io.github.uditkarode.able.services.SpotifyImportService
import io.github.uditkarode.able.utils.Shared
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.internal.toImmutableList

class PlaylistFragment : MediaItemFragment(), CreatePlaylistDialog.PlaylistCreatedCallback, MusicService.MusicClient {
    var binding by AutoClearedValue<FragmentPlaylistsBinding>(this)
    private lateinit var playlistAdapter: PlaylistAdapter
    private var isImporting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflater.inflateWithBinding(R.layout.fragment_playlists, container)
        return binding.root
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("RestrictedApi", "CheckResult")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        playlistAdapter = PlaylistAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(safeActivity)
            adapter = playlistAdapter
            addItemDecoration(DividerItemDecoration(safeActivity, VERTICAL).apply {
                setDrawable(safeActivity.drawable(R.drawable.divider)!!)
            })
            addOnItemClick { position, _ ->
                mainViewModel.mediaItemClicked(playlistAdapter.playlists[position], null)
            }
        }

        mediaItemFragmentViewModel.mediaItems
                .filter { it.isNotEmpty() }
                .observe(this) { list ->
                    @Suppress("UNCHECKED_CAST")
                    playlistAdapter.updateData(list as List<Playlist>)
                }

        binding.btnNewPlaylist.setOnClickListener {
            CreatePlaylistDialog.show(this@PlaylistFragment)
        }

        var inputId = ""
        binding.floatingActionButton.setOnClickListener {
            if (!isImporting) {
                MaterialDialog(requireContext()).show {
                    title(io.github.uditkarode.able.R.string.spot_title)
                    input(waitForPositiveButton = false) { dialog, textInp ->
                        val inputField = dialog.getInputField()
                        val validUrl =
                            textInp.toString().replace("https://", "")
                                .split("/").toMutableList()
                        var isValid = true
                        if (validUrl.size <= 2 || validUrl[0] != "open.spotify.com" || validUrl[1] != "playlist") {
                            isValid = false
                        } else if (validUrl[2].contains("?")) {
                            inputId = validUrl[2].split("?")[0]
                        } else {
                            inputId = validUrl[2]
                        }

                        inputField.error = if (isValid) null else getString(io.github.uditkarode.able.R.string.spot_invalid)
                        dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                    }
                    getInputLayout().boxBackgroundColor = Color.parseColor("#000000")
                    positiveButton(io.github.uditkarode.able.R.string.pos) {
                        val builder = Data.Builder()
                        builder.put("inputId", inputId)
                        WorkManager.getInstance(view.context)
                            .beginUniqueWork(
                                "SpotifyImport",
                                ExistingWorkPolicy.REPLACE,
                                OneTimeWorkRequest.Builder(SpotifyImportService::class.java)
                                    .setInputData(builder.build())
                                    .build()
                            ).enqueue()
                        Toast.makeText(
                            requireContext(), getString(io.github.uditkarode.able.R.string.spot_importing), Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                MusicService.registeredClients.forEach { it.spotifyImportChange(false) }
                WorkManager.getInstance(requireContext()).cancelUniqueWork("SpotifyImport")
            }
        }
    }

    override fun onPlaylistCreated() = mediaItemFragmentViewModel.reloadMediaItems()

    override fun playStateChanged(state: SongState) {}

    override fun songChanged() {}

    override fun durationChanged(duration: Int) {}

    override fun isExiting() {}

    override fun queueChanged(arrayList: ArrayList<Song>) {}

    override fun shuffleRepeatChanged(onShuffle: Boolean, onRepeat: Boolean) {}

    override fun indexChanged(index: Int) {}

    override fun isLoading(doLoad: Boolean) {}

    override fun spotifyImportChange(starting: Boolean) {
        if (starting) {
            isImporting = true
            binding.floatingActionButton.setImageResource(io.github.uditkarode.able.R.drawable.ic_cancle_action)
        } else {
            isImporting = false
            WorkManager.getInstance(requireContext()).cancelUniqueWork("SpotifyImport")
            (binding.recyclerView.adapter as PlaylistAdapter).also { playlistAdapter ->
                playlistAdapter.updateData(Shared.getPlaylists().toImmutableList() as List<Playlist>)
                binding.floatingActionButton.setImageResource(io.github.uditkarode.able.R.drawable.ic_spot)
            }
        }
    }

    override fun serviceStarted() {}
}
