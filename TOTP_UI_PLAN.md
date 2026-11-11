# Plan: UI changes for 2FA with an authenticator app (TOTP)

## Current state

The API layer supports the `totp` and `backup` methods:

- `ApiConnector.login` gives `method`, `methods` and `challenge_token` to the `TokenSupplier`. It does not examine the method.
- `ApiConnector.twoFactorAuthentication(method, code, challengeToken)` sends the method that it gets.
- `ApiConnector.resendVerificationCode` accepts only `email`. TOTP and backup codes are not sent by the server, thus there is nothing to resend.

The UI does not support the new methods yet:

- `CliTwoFactorTokenSupplier` accepts only `email`. For other methods it throws `Unsupported authentication method`. Also, the check `"email".contains(method)` is incorrect, because an empty method passes the check.
- `PopUpTwoFactorTokenSupplier` shows `Authentication method UNKNOWN: totp` and shows no instructions. The OK button works, so a user can type the app code. But the dialog does not tell the user what to do.

## Changes

### 1. Messages (`org.mnm.config.Messages.TwoFactorAuthentication`)

Add these instructions:

- `TOTP_INSTRUCTIONS`: "Enter the six-digit code from your authenticator app."
- `BACKUP_INSTRUCTIONS`: "Enter one of your backup codes. You can use each backup code only one time."

Add a method that gives the instructions for a method value (`email`, `totp`, `backup`), so that the CLI and the GUI use the same text. It returns `null` for an unknown method.

### 2. CLI (`CliTwoFactorTokenSupplier`)

- Use a `switch` on the method value in place of `"email".contains(method)`.
- For `email`, `totp` and `backup`: show the applicable instructions, read the code and call `twoFactorAuthentication`.
- For an unknown method: keep the `Unsupported authentication method` error.
- Optional: if `methods` has more than one item, let the user type a different method (for example, `backup` when the phone is not available).

### 3. GUI (`PopUpTwoFactorTokenSupplier.VerificationDialog`)

- Method label: show a clear name ("Email", "Authenticator app", "Backup code"), not the raw value.
- Instructions label: show the applicable text from `Messages`.
- Resend button: show it only for `email` (keep the `EMAIL_METHOD` constant). Thus there is no resend button for `totp` and `backup`.
- Method selection: when `methods` has more than one item, add a combo box with the methods. The server selects the default, and the combo box starts with it. When the user selects a different method:
  - Update the method label, the instructions and the resend button.
  - Use the selected method in `twoFactorAuthentication`.
  - When the user selects `email`, the server possibly did not send an email yet. In that case, call `resendVerificationCode` to send it. Do a check of this behavior with the real server before the implementation.
- Code field: for `totp`, accept digits only, maximum 6 characters. Backup codes can have a different format, thus do not use this limit for `backup`.
- Expiry (optional): use `expires_in` (300 seconds) to show the time that remains. When the time is over, show "The verification expired. Log in again." and close the dialog with `CancelException`. To do this, `TokenSupplier` must get `expires_in`. Possibly replace the three parameters with one `TwoFactorChallenge` record (`method`, `methods`, `challengeToken`, `expiresIn`).

### 4. Remove the TODO

Remove the TODO "implement support for others with a visitor or subclasses for each method" from `PopUpTwoFactorTokenSupplier`. The `switch` on the method value replaces it.

## Tests

- `CliTwoFactorTokenSupplierTest`: one test for each method (`email`, `totp`, `backup`) with `ApiServerStubs.stubTwoFactorAuthentication`. One test for an unknown method. One test for an empty method.
- `Messages`: one test that `email`, `totp` and `backup` have instructions, and that an unknown method has none.
- GUI: move the logic that selects the label, the instructions and the resend button out of the dialog into a class that does not use Swing. Test that class. Examples: `totp` gives no resend button, `email` gives a resend button.

## Documentation

- Update `docs/authentication.adoc`: the CLI supports `totp` and `backup`, and the GUI has the method selection.
