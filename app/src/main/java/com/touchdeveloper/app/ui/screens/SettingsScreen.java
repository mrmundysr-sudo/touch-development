package com.touchdeveloper.app.ui.screens;

import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.touchdeveloper.app.MainActivity;
import com.touchdeveloper.app.ServiceLocator;
import com.touchdeveloper.app.safety.Confirmations;
import com.touchdeveloper.app.ui.Screen;
import com.touchdeveloper.app.ui.Ui;

/**
 * Setup screen: connection status and secure credential entry, plus the activity
 * history required by the safety rules.
 *
 * Credentials are written straight into encrypted Android storage. They are never
 * displayed back in full, never logged, and never placed in a handoff ZIP.
 */
public class SettingsScreen extends Screen {

    private LinearLayout column;
    private EditText githubToken;
    private EditText openHandsEndpoint;
    private EditText openHandsToken;

    public SettingsScreen(MainActivity main) {
        super(main);
    }

    @Override
    public String title() {
        return "Setup and history";
    }

    @Override
    public View create() {
        column = main.column();

        column.addView(Ui.sectionLabel(main, "Connection mode"));
        column.addView(Ui.body(main, "GitHub: " + main.services().gitHub().modeDescription()));
        column.addView(Ui.body(main, "OpenHands: " + main.services().openHands().modeDescription()));
        column.addView(Ui.body(main, "Build: " + main.services().build().modeDescription()));

        column.addView(Ui.sectionLabel(main, "Stored credentials (never shown in full)"));
        column.addView(Ui.body(main, "GitHub token: "
                + main.services().credentials().maskedSummary(ServiceLocator.KEY_GITHUB_TOKEN)));
        column.addView(Ui.body(main, "OpenHands endpoint: "
                + main.services().credentials().maskedSummary(ServiceLocator.KEY_OPENHANDS_ENDPOINT)));
        column.addView(Ui.body(main, "OpenHands token: "
                + main.services().credentials().maskedSummary(ServiceLocator.KEY_OPENHANDS_TOKEN)));

        column.addView(Ui.sectionLabel(main, "Connect GitHub"));
        githubToken = passwordField("Paste a GitHub personal access token");
        column.addView(githubToken);
        Button saveGithub = Ui.button(main, "Save GitHub token securely");
        saveGithub.setOnClickListener(v -> saveCredential(ServiceLocator.KEY_GITHUB_TOKEN,
                githubToken, "GitHub token"));
        column.addView(saveGithub);
        Button clearGithub = Ui.button(main, "Remove GitHub token");
        clearGithub.setOnClickListener(v -> removeCredential(ServiceLocator.KEY_GITHUB_TOKEN,
                "GitHub token"));
        column.addView(clearGithub);

        column.addView(Ui.sectionLabel(main, "Connect OpenHands / build workspace"));
        openHandsEndpoint = textField("Endpoint URL, for example https://your-workspace.example");
        column.addView(openHandsEndpoint);
        openHandsToken = passwordField("Paste a workspace token (optional)");
        column.addView(openHandsToken);
        Button saveOpenHands = Ui.button(main, "Save OpenHands settings securely");
        saveOpenHands.setOnClickListener(v -> saveOpenHands());
        column.addView(saveOpenHands);
        Button clearOpenHands = Ui.button(main, "Remove OpenHands settings");
        clearOpenHands.setOnClickListener(v -> {
            removeCredential(ServiceLocator.KEY_OPENHANDS_ENDPOINT, "OpenHands endpoint");
            removeCredential(ServiceLocator.KEY_OPENHANDS_TOKEN, "OpenHands token");
        });
        column.addView(clearOpenHands);

        column.addView(Ui.sectionLabel(main, "Safety notes"));
        column.addView(Ui.body(main, "Credentials are encrypted with a key held in the Android Keystore. "
                + "They are never written to logs, source ZIPs, or handoff packages."));

        column.addView(Ui.sectionLabel(main, "Activity history"));
        TextView history = Ui.mono(main, main.services().activityLog().asText());
        column.addView(history);

        Button refresh = Ui.button(main, "Refresh history");
        refresh.setOnClickListener(v -> main.replace(new SettingsScreen(main)));
        column.addView(refresh);
        return column;
    }

    private EditText textField(String hint) {
        EditText field = new EditText(main);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setHint(hint);
        return field;
    }

    private EditText passwordField(String hint) {
        EditText field = new EditText(main);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setHint(hint);
        return field;
    }

    private void saveCredential(String key, EditText field, String label) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) {
            main.toast("Enter a value for " + label + " first.");
            return;
        }
        boolean stored = main.services().credentials().store(key, value);
        field.setText("");
        main.services().activityLog().record("setup", label + (stored ? " stored securely" : " was not stored"),
                false);
        main.services().rebuild();
        if (stored) {
            Confirmations.info(main, "Stored securely", label + " was stored in encrypted storage. "
                    + "The value is not shown again and is not included in any exported file.");
        } else {
            Confirmations.info(main, "Not stored", label + " could not be stored on this device. "
                    + "Check that secure storage is available.");
        }
        main.replace(new SettingsScreen(main));
    }

    private void saveOpenHands() {
        String endpoint = openHandsEndpoint.getText().toString().trim();
        String token = openHandsToken.getText().toString().trim();
        if (endpoint.isEmpty()) {
            main.toast("Enter an OpenHands endpoint first.");
            return;
        }
        boolean endpointStored = main.services().credentials()
                .store(ServiceLocator.KEY_OPENHANDS_ENDPOINT, endpoint);
        boolean tokenStored = token.isEmpty() || main.services().credentials()
                .store(ServiceLocator.KEY_OPENHANDS_TOKEN, token);
        openHandsToken.setText("");
        main.services().activityLog().record("setup",
                "OpenHands settings " + (endpointStored && tokenStored ? "stored securely" : "not stored"),
                false);
        main.services().rebuild();
        Confirmations.info(main, endpointStored && tokenStored ? "Stored securely" : "Not stored",
                "OpenHands endpoint and token were saved to encrypted storage. "
                        + "The endpoint contract is unverified, so build success is never assumed.");
        main.replace(new SettingsScreen(main));
    }

    private void removeCredential(String key, String label) {
        Confirmations.confirmDestructive(main, "Remove " + label + "?",
                "Remove the stored " + label + " from this device? Unsaved work is not affected.",
                () -> {
                    main.services().credentials().remove(key);
                    main.services().activityLog().record("setup", label + " removed", true);
                    main.services().rebuild();
                    main.replace(new SettingsScreen(main));
                });
    }
}
