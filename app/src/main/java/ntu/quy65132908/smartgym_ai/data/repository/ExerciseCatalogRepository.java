package ntu.quy65132908.smartgym_ai.data.repository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogFilters;
import ntu.quy65132908.smartgym_ai.data.model.ExerciseCatalogItem;

@Singleton
public class ExerciseCatalogRepository {
    private final List<ExerciseCatalogItem> seedItems;

    @Inject
    public ExerciseCatalogRepository() {
        this.seedItems = Collections.unmodifiableList(buildSeedItems());
    }

    public void search(String query, ExerciseCatalogFilters filters, CatalogCallback callback) {
        callback.onSuccess(searchSync(query, filters));
    }

    public List<ExerciseCatalogItem> searchSync(String query, ExerciseCatalogFilters filters) {
        String normalizedQuery = normalize(query);
        ExerciseCatalogFilters safeFilters = filters != null ? filters : ExerciseCatalogFilters.all();
        List<ExerciseCatalogItem> results = new ArrayList<>();
        for (ExerciseCatalogItem item : seedItems) {
            if (!matchesQuery(item, normalizedQuery)) {
                continue;
            }
            if (!matchesFilter(safeFilters.getPrimaryMuscle(), item.getPrimaryMuscle())) {
                continue;
            }
            if (!matchesFilter(safeFilters.getEquipment(), item.getEquipment())) {
                continue;
            }
            if (!matchesFilter(safeFilters.getDifficulty(), item.getDifficulty())) {
                continue;
            }
            if (safeFilters.isPoseSupportedOnly() && !item.isPoseSupported()) {
                continue;
            }
            results.add(item);
        }
        return results;
    }

    public List<ExerciseCatalogItem> getSeedItems() {
        return seedItems;
    }

    private boolean matchesQuery(ExerciseCatalogItem item, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        return normalize(item.getName()).contains(normalizedQuery)
                || normalize(item.getPrimaryMuscle()).contains(normalizedQuery)
                || normalize(item.getSafetyNote()).contains(normalizedQuery);
    }

    private boolean matchesFilter(String filter, String value) {
        return filter == null || filter.trim().isEmpty()
                || normalize(filter).equals(normalize(value));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private List<ExerciseCatalogItem> buildSeedItems() {
        List<ExerciseCatalogItem> items = new ArrayList<>();
        items.add(new ExerciseCatalogItem("push_up", "Chống đẩy", "Ngực", "Không dụng cụ", "Cơ bản", 3, 10, 60, "push_up", "Giữ thân người thẳng, không võng lưng."));
        items.add(new ExerciseCatalogItem("squat", "Squat", "Chân", "Không dụng cụ", "Cơ bản", 3, 12, 60, "squat", "Đẩy gối theo hướng mũi chân, không xuống quá sâu nếu đau gối."));
        items.add(new ExerciseCatalogItem("plank", "Plank", "Core", "Không dụng cụ", "Cơ bản", 3, 30, 45, "plank", "Siết bụng và giữ cổ trung lập."));
        items.add(new ExerciseCatalogItem("glute_bridge", "Glute bridge", "Mông", "Không dụng cụ", "Cơ bản", 3, 15, 45, "", "Ép mông ở điểm cao nhất, không ưỡn lưng quá mức."));
        items.add(new ExerciseCatalogItem("dead_bug", "Dead bug", "Core", "Không dụng cụ", "Cơ bản", 3, 10, 45, "", "Giữ lưng dưới áp sát sàn."));
        items.add(new ExerciseCatalogItem("dumbbell_row", "Kéo tạ đơn", "Lưng", "Tạ đơn", "Trung bình", 3, 12, 75, "", "Giữ vai thấp và kéo bằng lưng."));
        items.add(new ExerciseCatalogItem("overhead_press", "Đẩy vai tạ đơn", "Vai", "Tạ đơn", "Trung bình", 3, 10, 75, "", "Giảm tải nếu vai khó chịu."));
        items.add(new ExerciseCatalogItem("reverse_lunge", "Lunge lùi", "Chân", "Không dụng cụ", "Trung bình", 3, 10, 60, "", "Bước lùi ngắn nếu đầu gối nhạy cảm."));
        items.add(new ExerciseCatalogItem("band_pull_apart", "Kéo dây kháng lực ngang", "Lưng", "Dây kháng lực", "Cơ bản", 3, 15, 45, "", "Giữ cổ vai thư giãn."));
        items.add(new ExerciseCatalogItem("bird_dog", "Bird dog", "Core", "Không dụng cụ", "Cơ bản", 3, 10, 45, "", "Giữ hông cân bằng, không xoay người."));
        return items;
    }

    public interface CatalogCallback {
        void onSuccess(List<ExerciseCatalogItem> items);
        void onError(Exception e);
    }
}
