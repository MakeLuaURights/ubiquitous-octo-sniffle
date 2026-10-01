package dev.privatesign;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivateSign implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("PrivateSign");

    @Override
    public void onInitialize() {
        SignConfig.get();
        LOG.info("Private Sign loaded: placed signs get the preset text");
    }

    /** Writes the configured text onto a freshly placed sign. */
    public static void fill(SignBlockEntity sign) {
        SignConfig c = SignConfig.get();
        SignText text = new SignText();
        for (int i = 0; i < 4; i++) {
            text = text.withMessage(i, Text.literal(c.lines.get(i)));
        }
        sign.setText(text, true);
        if (c.backSide) sign.setText(text, false);
        if (c.wax) sign.setWaxed(true);
    }
}
