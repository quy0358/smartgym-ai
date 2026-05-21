package ntu.quy65132908.smartgym_ai.ui.progress;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import ntu.quy65132908.smartgym_ai.data.model.ProgressEntry;
import ntu.quy65132908.smartgym_ai.data.repository.AuthRepository;
import ntu.quy65132908.smartgym_ai.data.repository.ProgressRepository;

@HiltViewModel
public class ProgressViewModel extends ViewModel {
    private final ProgressRepository progressRepo;
    private final AuthRepository authRepo;
    private final MutableLiveData<List<ProgressEntry>> entries = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<ProgressEntry>> getEntries() { return entries; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    @Inject
    public ProgressViewModel(ProgressRepository progressRepo, AuthRepository authRepo) {
        this.progressRepo = progressRepo;
        this.authRepo = authRepo;
        loadProgress();
    }

    public void loadProgress() {
        FirebaseUser u = authRepo.getCurrentUser();
        if (u == null) return;
        isLoading.setValue(true);
        progressRepo.getHistory(u.getUid(), new ProgressRepository.ProgressCallback() {
            @Override
            public void onSuccess(List<ProgressEntry> list) { isLoading.postValue(false); entries.postValue(list); }
            @Override
            public void onError(Exception e) { isLoading.postValue(false); }
        });
    }
}
