package com.touchdeveloper.app.ui.screens;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.model.Repo;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.TaskRunner;
import com.touchdeveloper.app.ui.Ui;
import com.touchdeveloper.app.util.Result;

import java.util.List;

/**
 * Repository Dashboard: connected repositories with branch, latest commit, build
 * status, latest APK and source ZIP, status labels, and a refresh action. Each
 * repository row is clickable and opens the file screen.
 */
public class RepositoryDashboardScreen extends Screen {

    public RepositoryDashboardScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        return "Touch Developer";
    }

    @Override
    public View create() {
        LinearLayout column = main.column();
        column.addView(Ui.body(main, main.services().gitHub().modeDescription()));

        if (!main.services().gitHub().isConfigured()) {
            column.addView(Ui.body(main, "Connect a GitHub token in Setup to work with real repositories. "
                    + "Until then, demo repositories below are labelled [DEMO]."));
        }

        // A failed load leaves a persistent banner instead of a toast that can be
        // missed or overwritten while the screen reloads.
        if (!main.lastError().isEmpty()) {
            column.addView(Ui.error(main, main.lastError()));
        }

        Button refresh = Ui.button(main, "Refresh repositories");
        refresh.setOnClickListener(v -> loadRepositories(true));
        column.addView(refresh);

        renderRepos(column);
        return column;
    }

    @Override
    public void onShown() {
        // Auto-load once. Do not retry after a failure, or a dead network would
        // loop request after request while the user just watches progress dialogs.
        if (main.repos().isEmpty() && main.lastError().isEmpty()) {
            loadRepositories(false);
        }
    }

    private void loadRepositories(final boolean userInitiated) {
        TaskRunner.run(main, "GitHub", "Loading repositories\u2026",
                () -> main.services().gitHub().listRepositories(),
                result -> applyResult(result, userInitiated));
    }

    private void applyResult(Result<List<Repo>> result, boolean userInitiated) {
        main.services().activityLog().record("dashboard", "Refresh repositories (" + result.display() + ")",
                userInitiated);
        if (result.data != null) {
            main.repos().clear();
            main.repos().addAll(result.data);
        }
        if (result.ok || result.demo) {
            // Demo data is clearly labelled and is not an error.
            main.setLastError("");
        } else {
            // A real failure: keep the current list, never silently swap in demo
            // data, and show the reason so the user can act on it.
            if (main.services().gitHub().isConfigured()) {
                // Stale [DEMO] rows from an earlier unconfigured session are not real
                // GitHub repositories; drop them so they are not mistaken for a
                // successful live list.
                for (int i = main.repos().size() - 1; i >= 0; i--) {
                    if (main.repos().get(i).isDemo()) {
                        main.repos().remove(i);
                    }
                }
            }
            main.setLastError(result.display());
            if (userInitiated) {
                main.toast(result.display());
            }
        }
        replaceSelf();
    }

    private void replaceSelf() {
        // Rebuild the dashboard in place so the refreshed list is visible.
        main.replace(new RepositoryDashboardScreen(main));
    }

    private void renderRepos(LinearLayout column) {
        if (main.repos().isEmpty()) {
            column.addView(Ui.body(main, "No repositories loaded yet. Tap Refresh repositories."));
            return;
        }
        column.addView(Ui.sectionLabel(main, "Repositories (" + main.repos().size() + ")"));
        for (Repo repo : main.repos()) {
            column.addView(repoCard(repo));
        }
    }

    private View repoCard(Repo repo) {
        LinearLayout card = new LinearLayout(main);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#FFFFFF"));
        card.setPadding(Ui.dp(main, 12), Ui.dp(main, 10), Ui.dp(main, 12), Ui.dp(main, 10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Ui.dp(main, 12), Ui.dp(main, 4), Ui.dp(main, 12), Ui.dp(main, 4));
        card.setLayoutParams(params);
        card.setElevation(Ui.dp(main, 2));

        TextView name = new TextView(main);
        name.setText(repo.getFullName() + (repo.isDemo() ? "  [DEMO]" : ""));
        name.setTextSize(16f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(name);

        card.addView(Ui.body(main, "Branch: " + repo.getCurrentBranch()));
        card.addView(Ui.body(main, "Latest commit: " + repo.shortCommit()
                + (repo.getLatestCommitMessage() == null ? "" : " - " + repo.getLatestCommitMessage())));
        card.addView(Ui.status(main, repo.getBuildStatus()));
        card.addView(Ui.body(main, "Latest APK: "
                + (repo.getLatestApkName() == null ? "none" : repo.getLatestApkName())));
        card.addView(Ui.body(main, "Latest source ZIP: "
                + (repo.getLatestSourceZipName() == null ? "none" : repo.getLatestSourceZipName())));

        card.setClickable(true);
        card.setOnClickListener(v -> {
            main.setSelectedRepo(repo);
            main.pendingRequest().setRepoFullName(repo.getFullName()).setBranch(repo.getCurrentBranch())
                    .setProjectName(repo.getName());
            main.services().activityLog().record("dashboard", "Opened " + repo.getFullName(), false);
            main.show(new FileScreen(main));
        });
        return card;
    }
}
