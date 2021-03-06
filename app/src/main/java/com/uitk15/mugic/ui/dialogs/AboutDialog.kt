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
package com.uitk15.mugic.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.uitk15.mugic.R

class AboutDialog : DialogFragment() {

    companion object {
        private const val TAG = "AboutDialog"

        fun show(activity: FragmentActivity) = AboutDialog().show(activity.supportFragmentManager, TAG)
    }

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog(requireActivity()).show {
            title(R.string.about_dialog_title)
            message(R.string.about_dialog_body)
            positiveButton(R.string.about_dialog_dismiss)
            onDismiss {
                // Make sure the DialogFragment dismisses as well
                this@AboutDialog.dismiss()
            }
        }
    }
}
