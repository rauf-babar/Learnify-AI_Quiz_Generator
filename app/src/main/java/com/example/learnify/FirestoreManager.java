package com.example.learnify;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    private final FirebaseFirestore db;
    private final Gson gson;

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
        gson = new Gson();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    public void saveUserProfile(String name, String email) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", name);
        user.put("email", email);
        user.put("lastLoginAt", System.currentTimeMillis());
        user.put("totalQuizzesTaken", 0);
        user.put("totalQuestionsSolved", 0);
        user.put("totalCorrectAnswers", 0);

        db.collection("users").document(uid).set(user, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user profile", e));
    }

    public void updateUserName(String newName, OnUpdateListener listener) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void deleteUserAccount(OnUpdateListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        db.collection("quiz_history")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.delete(db.collection("users").document(uid));

                    batch.commit().addOnSuccessListener(aVoid -> user.delete()
                            .addOnSuccessListener(unused -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onFailure(e.getMessage()))).addOnFailureListener(e -> listener.onFailure(e.getMessage()));

                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getUserName(String uid, OnNameLoadedListener listener) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        listener.onNameLoaded(name != null ? name : "User");
                    } else {
                        listener.onNameLoaded("User");
                    }
                })
                .addOnFailureListener(e -> listener.onNameLoaded("User"));
    }

    public void checkUserExists(String uid, OnUserCheckListener listener) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> listener.onUserFound(documentSnapshot.exists()))
                .addOnFailureListener(e -> listener.onUserFound(false));
    }

    public void saveQuizResult(QuizRecord record, QuizResult fullResult) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String historyId = record.getQuizId();

        Map<String, Object> historyMap = new HashMap<>();
        historyMap.put("historyId", historyId);
        historyMap.put("uid", uid);
        historyMap.put("topicName", record.getTopicName());
        historyMap.put("sourceType", record.getSource());
        historyMap.put("sourceData", record.getSourceData());
        historyMap.put("totalQuestions", record.getTotalQuestions());
        historyMap.put("correctAnswers", record.getCorrectAnswers());
        historyMap.put("accuracy", record.getAccuracyPercentage());
        historyMap.put("timeTakenMs", record.getTimeTakenMs());
        historyMap.put("completedAt", record.getCompletedAt());
        historyMap.put("status", "COMPLETED");
        historyMap.put("difficulty", record.getDifficulty());

        historyMap.put("quizData_json", gson.toJson(fullResult));

        WriteBatch batch = db.batch();

        DocumentReference historyRef = db.collection("quiz_history").document(historyId);
        batch.set(historyRef, historyMap);

        DocumentReference userRef = db.collection("users").document(uid);

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("totalQuizzesTaken", FieldValue.increment(1));
        userUpdates.put("totalQuestionsSolved", FieldValue.increment(record.getTotalQuestions()));
        userUpdates.put("totalCorrectAnswers", FieldValue.increment(record.getCorrectAnswers()));

        batch.set(userRef, userUpdates, SetOptions.merge());

        batch.commit().addOnFailureListener(e -> Log.e(TAG, "Error saving quiz to cloud", e));
    }

    public void getAllQuizHistory(String uid, OnHistoryLoadedListener listener) {
        db.collection("quiz_history")
                .whereEqualTo("uid", uid)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<QuizRecord> records = new ArrayList<>();
                    List<String> rawJsons = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String diff = doc.getString("difficulty");
                        if (diff == null) diff = "Medium";

                        QuizRecord record = new QuizRecord(
                                doc.getString("historyId"),
                                doc.getString("uid"),
                                doc.getString("topicName"),
                                doc.getLong("totalQuestions").intValue(),
                                doc.getLong("correctAnswers").intValue(),
                                doc.getLong("timeTakenMs"),
                                doc.getString("sourceType"),
                                doc.getString("sourceData"),
                                doc.getLong("completedAt"),
                                diff
                        );
                        records.add(record);
                        rawJsons.add(doc.getString("quizData_json"));
                    }
                    listener.onHistoryLoaded(records, rawJsons);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnNameLoadedListener { void onNameLoaded(String name); }
    public interface OnUserCheckListener { void onUserFound(boolean exists); }
    public interface OnUpdateListener {
        void onSuccess();
        void onFailure(String error);
    }
    public interface OnHistoryLoadedListener {
        void onHistoryLoaded(List<QuizRecord> records, List<String> rawJsons);
        void onError(String error);
    }
}