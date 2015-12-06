package com.moez.QKSMS.common;

import android.util.Log;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.moez.QKSMS.R;
import com.moez.QKSMS.data.Conversation;
import com.moez.QKSMS.transaction.SmsHelper;
import com.moez.QKSMS.ui.MainActivity;
import com.moez.QKSMS.ui.base.QKActivity;
import com.moez.QKSMS.ui.dialog.DefaultSmsHelper;
import com.moez.QKSMS.ui.dialog.QKDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class DialogHelper {
    private static final String TAG = "DialogHelper";

    public static void showDeleteConversationDialog(MainActivity context, long threadId) {
        Set<Long> threadIds = new HashSet<>();
        threadIds.add(threadId);
        showDeleteConversationsDialog(context, threadIds);
    }

    public static void showDeleteConversationsDialog(final MainActivity context, final Set<Long> threadIds) {
        new DefaultSmsHelper(context, R.string.not_default_delete).showIfNotDefault(null);

        Set<Long> threads = new HashSet<>(threadIds); // Make a copy so the list isn't reset when multi-select is disabled
        new QKDialog()
                .setContext(context)
                .setTitle(R.string.delete_conversation)
                .setMessage(context.getString(R.string.delete_confirmation, threads.size()))
                .setPositiveButton(R.string.yes, v -> {
                    Log.d(TAG, "Deleting threads: " + Arrays.toString(threads.toArray()));
                    Conversation.ConversationQueryHandler handler = new Conversation.ConversationQueryHandler(context.getContentResolver());
                    Conversation.startDelete(handler, 0, false, threads);
                    Conversation.asyncDeleteObsoleteThreads(handler, 0);
                    context.showMenu();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();

    }

    public static void showDeleteFailedMessagesDialog(final MainActivity context, final Set<Long> threadIds) {
        new DefaultSmsHelper(context, R.string.not_default_delete).showIfNotDefault(null);

        Set<Long> threads = new HashSet<>(threadIds); // Make a copy so the list isn't reset when multi-select is disabled
        new QKDialog()
                .setContext(context)
                .setTitle(R.string.delete_all_failed)
                .setMessage(context.getString(R.string.delete_all_failed_confirmation, threads.size()))
                .setPositiveButton(R.string.yes, v -> {
                    new Thread(() -> {
                        for (long threadId : threads) {
                            SmsHelper.deleteFailedMessages(context, threadId);
                        }
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void showChangelog(QKActivity context) {
        context.showProgressDialog();

        String url = "https://qksms-changelog.firebaseio.com/changes.json";

        StringRequest request = new StringRequest(url, response -> {
            Gson gson = new Gson();
            Change[] changes = gson.fromJson(response, Change[].class);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat formatWithRevision = new SimpleDateFormat("yyyy-MM-dd-'r'H"); // For multiple updates in a day
            for (Change change : changes) {
                try {
                    if (change.date.length() > 11) {
                        change.dateLong = formatWithRevision.parse(change.date).getTime();
                    } else {
                        change.dateLong = format.parse(change.date).getTime();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            Arrays.sort(changes, (lhs, rhs) -> Long.valueOf(rhs.dateLong).compareTo(lhs.dateLong));

            context.hideProgressDialog();
        }, error -> {
            context.hideProgressDialog();
            context.makeToast(R.string.toast_changelog_error);
        });

        context.getRequestQueue().add(request);
    }

    private class Change implements Comparator<Long> {
        @SerializedName("changes") ArrayList<String> changes;
        @SerializedName("release_date") String date;
        @SerializedName("version_name") String version;

        long dateLong = 0;

        @Override
        public int compare(Long lhs, Long rhs) {
            return lhs.compareTo(rhs);
        }
    }

}
