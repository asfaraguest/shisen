package id.shisen.app.ui.player;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import id.shisen.app.data.model.Episode;

import static org.junit.Assert.*;

public class EpisodeNavigationTest {

    private int resolveNextIndex(List<Episode> episodeList, int currentIndex, boolean isSortAscending) {
        if (episodeList == null || episodeList.isEmpty() || currentIndex < 0 || currentIndex >= episodeList.size()) return -1;
        Episode curr = episodeList.get(currentIndex);
        for (int i = 0; i < episodeList.size(); i++) {
            if (episodeList.get(i).number == curr.number + 1) {
                return i;
            }
        }
        if (isSortAscending) {
            return (currentIndex + 1 < episodeList.size()) ? currentIndex + 1 : -1;
        } else {
            return (currentIndex - 1 >= 0) ? currentIndex - 1 : -1;
        }
    }

    private int resolvePrevIndex(List<Episode> episodeList, int currentIndex, boolean isSortAscending) {
        if (episodeList == null || episodeList.isEmpty() || currentIndex < 0 || currentIndex >= episodeList.size()) return -1;
        Episode curr = episodeList.get(currentIndex);
        for (int i = 0; i < episodeList.size(); i++) {
            if (episodeList.get(i).number == curr.number - 1) {
                return i;
            }
        }
        if (isSortAscending) {
            return (currentIndex - 1 >= 0) ? currentIndex - 1 : -1;
        } else {
            return (currentIndex + 1 < episodeList.size()) ? currentIndex + 1 : -1;
        }
    }

    @Test
    public void testAscendingEpisodeNavigation() {
        List<Episode> episodes = new ArrayList<>();
        episodes.add(new Episode(1));
        episodes.add(new Episode(2));
        episodes.add(new Episode(3));

        // Episode 1 (index 0)
        assertEquals(1, resolveNextIndex(episodes, 0, true)); // Next -> Ep 2 (index 1)
        assertEquals(-1, resolvePrevIndex(episodes, 0, true)); // Prev -> none

        // Episode 2 (index 1)
        assertEquals(2, resolveNextIndex(episodes, 1, true)); // Next -> Ep 3 (index 2)
        assertEquals(0, resolvePrevIndex(episodes, 1, true)); // Prev -> Ep 1 (index 0)

        // Episode 3 (index 2)
        assertEquals(-1, resolveNextIndex(episodes, 2, true)); // Next -> none (last episode)
        assertEquals(1, resolvePrevIndex(episodes, 2, true)); // Prev -> Ep 2 (index 1)
    }

    @Test
    public void testDescendingReversedEpisodeNavigation() {
        List<Episode> episodes = new ArrayList<>();
        episodes.add(new Episode(3));
        episodes.add(new Episode(2));
        episodes.add(new Episode(1));

        // In reversed list, index 0 is Ep 3, index 1 is Ep 2, index 2 is Ep 1
        // When watching Ep 1 (index 2): Next chronologically is Ep 2 (index 1), Prev is none
        assertEquals(1, resolveNextIndex(episodes, 2, false));
        assertEquals(-1, resolvePrevIndex(episodes, 2, false));

        // When watching Ep 2 (index 1): Next is Ep 3 (index 0), Prev is Ep 1 (index 2)
        assertEquals(0, resolveNextIndex(episodes, 1, false));
        assertEquals(2, resolvePrevIndex(episodes, 1, false));

        // When watching Ep 3 (index 0): Next is none, Prev is Ep 2 (index 1)
        assertEquals(-1, resolveNextIndex(episodes, 0, false));
        assertEquals(1, resolvePrevIndex(episodes, 0, false));
    }

    @Test
    public void testOutOfBoundsAndEmptyCases() {
        List<Episode> empty = new ArrayList<>();
        assertEquals(-1, resolveNextIndex(empty, 0, true));
        assertEquals(-1, resolvePrevIndex(empty, 0, true));

        List<Episode> single = Collections.singletonList(new Episode(1));
        assertEquals(-1, resolveNextIndex(single, 0, true));
        assertEquals(-1, resolvePrevIndex(single, 0, true));
        assertEquals(-1, resolveNextIndex(single, 99, true));
        assertEquals(-1, resolvePrevIndex(single, -1, true));
    }
}
