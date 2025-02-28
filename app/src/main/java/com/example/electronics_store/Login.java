package com.example.electronics_store;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.*;
import java.io.IOException;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class Login extends AppCompatActivity {

  private static final int RC_SIGN_IN = 9001;
  private GoogleSignInClient mGoogleSignInClient;
  DatabaseReference databaseReference = FirebaseDatabase
    .getInstance()
    .getReferenceFromUrl("https://techstore-81b9b-default-rtdb.firebaseio.com/");
  private static final String CLIENT_ID =
    "839467012966-9mdql4q3palef3stneh2dgp41jl4g35a.apps.googleusercontent.com";
  private static final String CLIENT_SECRET =
    "839467012966-9mdql4q3palef3stneh2dgp41jl4g35a.apps.googleusercontent.com";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.signin);

    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
      GoogleSignInOptions.DEFAULT_SIGN_IN
    )
      .requestEmail()
      .requestServerAuthCode(CLIENT_ID)
      .build();
    mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    final EditText email = findViewById(R.id.email);
    final EditText password = findViewById(R.id.password);
    final Button loginBtn = findViewById(R.id.continue_button);
    final TextView registerBtn = findViewById(R.id.signupNow);
    final LinearLayout googleSignInContainer = findViewById(
      R.id.google_signin_container
    );

    loginBtn.setOnClickListener(v -> {
      final String emailTxt = email.getText().toString();
      final String passwordTxt = password.getText().toString();
      if (emailTxt.isEmpty() || passwordTxt.isEmpty()) {
        Toast
          .makeText(
            Login.this,
            "Enter your email or password!!",
            Toast.LENGTH_SHORT
          )
          .show();
      } else {
        String emailKey = emailTxt.replace(".", ",");
        databaseReference
          .child("users")
          .addListenerForSingleValueEvent(
            new ValueEventListener() {
              @Override
              public void onDataChange(@NotNull DataSnapshot snapshot) {
                if (snapshot.hasChild(emailKey)) {
                  final String getPassword = snapshot
                    .child(emailKey)
                    .child("password")
                    .getValue(String.class);
                  String role = snapshot
                    .child(emailKey)
                    .child("role")
                    .getValue(String.class);
                  if (getPassword != null && getPassword.equals(passwordTxt)) {
                    Toast
                      .makeText(
                        Login.this,
                        "Login successfully!!",
                        Toast.LENGTH_SHORT
                      )
                      .show();
                    redirectBasedOnRole(role);
                  } else {
                    Toast
                      .makeText(
                        Login.this,
                        "Try again! Wrong email or password!!",
                        Toast.LENGTH_SHORT
                      )
                      .show();
                  }
                } else {
                  Toast
                    .makeText(
                      Login.this,
                      "Try again! Wrong email or password!!",
                      Toast.LENGTH_SHORT
                    )
                    .show();
                }
              }

              @Override
              public void onCancelled(DatabaseError error) {
                Toast
                  .makeText(
                    Login.this,
                    "Database error: " + error.getMessage(),
                    Toast.LENGTH_SHORT
                  )
                  .show();
              }
            }
          );
      }
    });

    googleSignInContainer.setOnClickListener(v -> signInWithGoogle());
    registerBtn.setOnClickListener(v ->
      startActivity(new Intent(Login.this, Register.class))
    );
  }

  private void redirectBasedOnRole(String role) {
    if ("admin".equals(role)) {
      startActivity(new Intent(Login.this, AdminActivity.class));
    } else {
      startActivity(new Intent(Login.this, MainActivity.class));
    }
    finish();
  }

  private void signInWithGoogle() {
    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
    startActivityForResult(signInIntent, RC_SIGN_IN);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RC_SIGN_IN) {
      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(
        data
      );
      try {
        GoogleSignInAccount account = task.getResult(ApiException.class);
        handleGoogleSignInResult(account);
      } catch (ApiException e) {
        Log.w(
          "Google Sign-In",
          "signInResult:failed code=" +
          e.getStatusCode() +
          ", message=" +
          e.getMessage()
        );
        Toast
          .makeText(
            this,
            "Google Sign-In failed: " + e.getStatusCode(),
            Toast.LENGTH_SHORT
          )
          .show();
      }
    }
  }

  private void handleGoogleSignInResult(GoogleSignInAccount account) {
    String email = account.getEmail();
    String authCode = account.getServerAuthCode();
    Log.d("GoogleSignIn", "Email: " + email + ", AuthCode: " + authCode);

    if (authCode != null) {
      exchangeAuthCodeForToken(authCode);
    } else {
      Toast.makeText(this, "No auth code received", Toast.LENGTH_SHORT).show();
    }
  }

  private void exchangeAuthCodeForToken(String authCode) {
    OkHttpClient client = new OkHttpClient();
    RequestBody formBody = new FormBody.Builder()
      .add("grant_type", "authorization_code")
      .add("client_id", CLIENT_ID)
      .add("client_secret", CLIENT_SECRET)
      .add("code", authCode)
      .add("redirect_uri", "")
      .build();

    Request request = new Request.Builder()
      .url("https://oauth2.googleapis.com/token")
      .post(formBody)
      .build();

    client
      .newCall(request)
      .enqueue(
        new Callback() {
          @Override
          public void onFailure(Call call, IOException e) {
            runOnUiThread(() ->
              Toast
                .makeText(
                  Login.this,
                  "Token exchange failed: " + e.getMessage(),
                  Toast.LENGTH_SHORT
                )
                .show()
            );
          }

          @Override
          public void onResponse(
            @NotNull Call call,
            @NotNull Response response
          ) throws IOException {
            if (response.isSuccessful()) {
              String responseData = response.body().string();
              try {
                JSONObject json = new JSONObject(responseData);
                String accessToken = json.getString("access_token");
                verifyTokenWithGoogle(accessToken);
              } catch (Exception e) {
                runOnUiThread(() ->
                  Toast
                    .makeText(
                      Login.this,
                      "Error parsing token response",
                      Toast.LENGTH_SHORT
                    )
                    .show()
                );
              }
            } else {
              runOnUiThread(() ->
                Toast
                  .makeText(
                    Login.this,
                    "Token exchange failed: " + response.code(),
                    Toast.LENGTH_SHORT
                  )
                  .show()
              );
            }
          }
        }
      );
  }

  private void verifyTokenWithGoogle(String accessToken) {
    OkHttpClient client = new OkHttpClient();
    String url =
      "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" +
      accessToken;

    Request request = new Request.Builder().url(url).build();

    client
      .newCall(request)
      .enqueue(
        new Callback() {
          @Override
          public void onFailure(@NotNull Call call, @NotNull IOException e) {
            runOnUiThread(() ->
              Toast
                .makeText(
                  Login.this,
                  "Token verification failed: " + e.getMessage(),
                  Toast.LENGTH_SHORT
                )
                .show()
            );
          }

          @Override
          public void onResponse(
            @NotNull Call call,
            @NotNull Response response
          ) throws IOException {
            if (response.isSuccessful()) {
              String responseData = response.body().string();
              try {
                JSONObject json = new JSONObject(responseData);
                String email = json.getString("email");
                runOnUiThread(() -> {
                  Toast
                    .makeText(
                      Login.this,
                      "Verified email: " + email,
                      Toast.LENGTH_SHORT
                    )
                    .show();
                  startActivity(new Intent(Login.this, MainActivity.class));
                  finish();
                });
              } catch (Exception e) {
                runOnUiThread(() ->
                  Toast
                    .makeText(
                      Login.this,
                      "Error parsing token info",
                      Toast.LENGTH_SHORT
                    )
                    .show()
                );
              }
            } else {
              runOnUiThread(() ->
                Toast
                  .makeText(
                    Login.this,
                    "Invalid token: " + response.code(),
                    Toast.LENGTH_SHORT
                  )
                  .show()
              );
            }
          }
        }
      );
  }
}
