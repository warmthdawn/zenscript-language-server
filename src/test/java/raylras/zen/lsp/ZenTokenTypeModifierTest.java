package raylras.zen.lsp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ZenTokenTypeModifierTest {

    @Test
    void getInt() {

        int actual = ZenTokenTypeModifier.getInt(new ZenTokenTypeModifier[]{ZenTokenTypeModifier.Static, ZenTokenTypeModifier.Definition});
        int expected = ZenTokenTypeModifier.Static.getId() | ZenTokenTypeModifier.Definition.getId();
        Assertions.assertEquals(expected, actual);

        actual = ZenTokenTypeModifier.getInt(Arrays.asList(ZenTokenTypeModifier.Static, ZenTokenTypeModifier.Definition, ZenTokenTypeModifier.Readonly));
        expected = 1 | 2 | 4;
        Assertions.assertEquals(expected, actual);

        actual = ZenTokenTypeModifier.getInt(new ZenTokenTypeModifier[0]);
        expected = 0;
        Assertions.assertEquals(expected, actual);

    }


}