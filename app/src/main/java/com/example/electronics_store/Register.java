package com.example.electronics_store;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Register extends AppCompatActivity {

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://techstore-81b9b-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        final EditText yourName = findViewById(R.id.name);
        final EditText emailAddress = findViewById(R.id.email);
        final EditText password = findViewById(R.id.password);
        final EditText confirmPassword = findViewById(R.id.reenter_password);
        final Button registerBtn = findViewById(R.id.signup_button);
        final TextView loginBtnNow = findViewById(R.id.signin_textview);


        registerBtn.setOnClickListener(v -> {
            try {
                final String nameText = yourName.getText().toString();
                final String emailText = emailAddress.getText().toString();
                final String passwordText = password.getText().toString();
                final String confirmPasswordText = confirmPassword.getText().toString();

                if (nameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
                    Toast.makeText(Register.this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!passwordText.equals(confirmPasswordText)) {
                    Toast.makeText(Register.this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
                    return;
                }

                String emailKey = emailText.replace(".", ",");

                databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChild(emailKey)) {
                            Toast.makeText(Register.this, "Email has already used!", Toast.LENGTH_SHORT).show();
                        } else {
                            databaseReference.child("users").child(emailKey).child("yourName").setValue(nameText);
                            databaseReference.child("users").child(emailKey).child("emailAddress").setValue(emailText);
                            databaseReference.child("users").child(emailKey).child("password").setValue(passwordText);
                            databaseReference.child("users").child(emailKey).child("role").setValue("user")
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(Register.this, "Registration successfully", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(Register.this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Register.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(Register.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        loginBtnNow.setOnClickListener(v -> finish());
    }
}
