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

        Button refresh = Ui.button(main, "Refresh repositories");
        refresh.setOnClickListener(v -> loadRepositories(true));
        column.addView(refresh);

        renderRepos(column);
        return column;
    }

    @Override
    public void onShown() {
        if (main.repos().isEmpty()) {
            loadRepositories(false);
        }
    }

    private void loadRepositories(boolean userInitiated) {
        Result<List<Repo>> result = main.services().gitHub().listRepositories();
        main.services().activityLog().record("dashboard", "Refresh repositories (" + result.display() + ")",
                userInitiated);
        if (result.data != null) {
            main.repos().clear();
            main.repos().addAll(result.data);
        }
        if (!result.ok) {
            main.toast(result.display());
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
