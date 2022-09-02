/*
 * Copyright (c) 2022 Hugo Dupanloup (Yeregorix)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.smoofyuniverse.fsg;

import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.fonts.IGlyphInfo;
import net.minecraft.client.gui.fonts.providers.IGlyphProvider;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

@Mod("font-sizes-generator")
public class FSGMod {
    private static final Logger LOGGER = LogManager.getLogger();

    public FSGMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        LOGGER.info("Hello.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fsg").executes(context -> {
            context.getSource().sendSuccess(new StringTextComponent("Generating font-sizes.json ..."), false);
            generateFontSizes();
            context.getSource().sendSuccess(new StringTextComponent("Done."), false);
            return 1;
        }));
    }

    public static void generateFontSizes() {
        Object2ObjectMap<CharWidth, IntList> widthsToCodes = new Object2ObjectArrayMap<>();
        IntSet allCodes = new IntOpenHashSet();

        for (IGlyphProvider provider : Minecraft.getInstance().font.getFontSet(Style.DEFAULT_FONT).providers) {
            IntIterator it = provider.getSupportedGlyphs().iterator();
            while (it.hasNext()) {
                int code = it.nextInt();
                if (allCodes.add(code)) {
                    IGlyphInfo glyph = provider.getGlyph(code);
                    float base = glyph.getAdvance();
                    CharWidth width = new CharWidth(base, base + glyph.getBoldOffset());
                    widthsToCodes.computeIfAbsent(width, k -> new IntArrayList()).add(code);
                }
            }
        }

        try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(Paths.get("font-sizes.json")))) {
            w.setIndent(" ");

            w.beginArray();

            for (Map.Entry<CharWidth, IntList> e : widthsToCodes.entrySet()) {
                CharWidth width = e.getKey();
                int[] codes = e.getValue().toIntArray();
                Arrays.sort(codes);

                StringBuilder b = new StringBuilder();
                for (int code : codes) {
                    b.appendCodePoint(code);
                }
                String chars = b.toString();

                w.beginObject();

                w.name("normal");
                w.value(width.normal);

                w.name("bold");
                w.value(width.bold);

                w.name("chars");
                w.value(chars);

                w.endObject();
            }

            w.endArray();
        } catch (IOException e) {
            LOGGER.error("Failed to write font sizes", e);
        }
    }
}
