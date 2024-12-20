package my.tablesalt.tablesalt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class ChefRegistration extends AppCompatActivity {

    // UI elements
    TextInputLayout Fname, Lname, Email, Pass, cpass;
    Spinner Statespin, Cityspin;
    Button signup;

    // Firebase references
    FirebaseAuth FAuth;
    DatabaseReference databaseReference;

    // User input data
    String fname, lname, email, password, confpassword, state, city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chef_registration);

        // Initialize UI elements
        Fname = findViewById(R.id.Firstname);
        Lname = findViewById(R.id.Lastname);
        Email = findViewById(R.id.Email);
        Pass = findViewById(R.id.Pwd);
        cpass = findViewById(R.id.Cpass);
        Statespin = findViewById(R.id.Statee);
        Cityspin = findViewById(R.id.Citys);
        signup = findViewById(R.id.Signup);

        // Initialize Firebase
        FAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Chef");

        // Populate State Spinner
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(
                this, R.array.State, android.R.layout.simple_spinner_item);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Statespin.setAdapter(stateAdapter);

        // Handle State Selection
        Statespin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                state = parent.getItemAtPosition(position).toString(); // Capture selected state
                ArrayList<String> cityList = new ArrayList<>();

                // Populate city list based on selected state
                if (state.equals("Dhaka")) {
                    String[] cities = getResources().getStringArray(R.array.DhakaCities);
                    for (String city : cities) {
                        cityList.add(city);
                    }
                } else if (state.equals("Chittagong")) {
                    String[] cities = getResources().getStringArray(R.array.ChittagongCities);
                    for (String city : cities) {
                        cityList.add(city);
                    }
                }

                // Set City Spinner adapter
                ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(ChefRegistration.this, android.R.layout.simple_spinner_item, cityList);
                cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                Cityspin.setAdapter(cityAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Handle City Selection
        Cityspin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                city = parent.getItemAtPosition(position).toString(); // Capture selected city
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Handle Signup Button Click
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Collect input from UI elements
                fname = Fname.getEditText().getText().toString().trim();
                lname = Lname.getEditText().getText().toString().trim();
                email = Email.getEditText().getText().toString().trim();
                password = Pass.getEditText().getText().toString().trim();
                confpassword = cpass.getEditText().getText().toString().trim();

                // Validate inputs before proceeding
                if (validateInputs()) {
                    registerUser(); // Proceed to register user in Firebase
                }
            }
        });
    }

    // Method to validate user inputs
    private boolean validateInputs() {
        if (TextUtils.isEmpty(fname)) {
            Fname.setError("Enter First Name");
            return false;
        }
        if (TextUtils.isEmpty(lname)) {
            Lname.setError("Enter Last Name");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            Email.setError("Enter Email");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            Pass.setError("Password must be at least 6 characters");
            return false;
        }
        if (!password.equals(confpassword)) {
            cpass.setError("Passwords do not match");
            return false;
        }
        if (TextUtils.isEmpty(state)) {
            Toast.makeText(this, "Please select a state", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(city)) {
            Toast.makeText(this, "Please select a city", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Method to register user in Firebase
    private void registerUser() {
        FAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = FAuth.getCurrentUser();
                if (firebaseUser != null) {
                    firebaseUser.sendEmailVerification().addOnCompleteListener(emailTask -> {
                        if (emailTask.isSuccessful()) {
                            String userId = firebaseUser.getUid(); // Get authenticated user's unique ID
                            DatabaseReference userRef = databaseReference.child(userId); // Create a child node for this user

                            HashMap<String, String> userMap = new HashMap<>();
                            userMap.put("First Name", fname);
                            userMap.put("Last Name", lname);
                            userMap.put("Email", email);
                            userMap.put("State", state);
                            userMap.put("City", city);

                            // Save user data in Firebase Realtime Database
                            userRef.setValue(userMap).addOnSuccessListener(aVoid -> {
                                Toast.makeText(ChefRegistration.this, "Registered successfully! Please verify your email.", Toast.LENGTH_LONG).show();
                                FAuth.signOut(); // Sign out user to force email verification
                            }).addOnFailureListener(e -> Toast.makeText(ChefRegistration.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(ChefRegistration.this, "Failed to send verification email: " + emailTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(ChefRegistration.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
