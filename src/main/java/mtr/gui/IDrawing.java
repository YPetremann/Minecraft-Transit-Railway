package mtr.gui;

import mtr.MTR;
import mtr.config.Config;
import mtr.data.IGui;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3i;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

import java.util.ArrayList;
import java.util.List;

public interface IDrawing {

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, VertexConsumerProvider.Immediate immediate, String text, float x, float y, int light) {
		drawStringWithFont(matrices, textRenderer, immediate, text, IGui.HorizontalAlignment.CENTER, IGui.VerticalAlignment.CENTER, x, y, 1, IGui.ARGB_WHITE, true, light, null);
	}

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, VertexConsumerProvider.Immediate immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, float x, float y, float scale, int textColor, boolean shadow, int light, IGui.DrawingCallback drawingCallback) {
		drawStringWithFont(matrices, textRenderer, immediate, text, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, -1, -1, scale, textColor, shadow, light, drawingCallback);
	}

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, VertexConsumerProvider.Immediate immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, IGui.DrawingCallback drawingCallback) {
		drawStringWithFont(matrices, textRenderer, immediate, text, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, maxWidth, maxHeight, scale, textColor, shadow, light, drawingCallback);
	}

	static void drawStringWithFont(MatrixStack matrices, TextRenderer textRenderer, VertexConsumerProvider.Immediate immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, IGui.DrawingCallback drawingCallback) {
		final Style style = Config.useMTRFont() ? Style.EMPTY.withFont(new Identifier(MTR.MOD_ID, "mtr")) : Style.EMPTY;

		while (text.contains("||")) {
			text = text.replace("||", "|");
		}
		final String[] stringSplit = text.split("\\|");

		final List<Boolean> isCJKList = new ArrayList<>();
		final List<OrderedText> orderedTexts = new ArrayList<>();
		int totalHeight = 0, totalWidth = 0;
		for (final String stringSplitPart : stringSplit) {
			final boolean isCJK = stringSplitPart.codePoints().anyMatch(Character::isIdeographic);
			isCJKList.add(isCJK);

			final OrderedText orderedText = new LiteralText(stringSplitPart).fillStyle(style).asOrderedText();
			orderedTexts.add(orderedText);

			totalHeight += IGui.LINE_HEIGHT * (isCJK ? 2 : 1);
			final int width = textRenderer.getWidth(orderedText) * (isCJK ? 2 : 1);
			if (width > totalWidth) {
				totalWidth = width;
			}
		}

		if (maxHeight >= 0 && totalHeight / scale > maxHeight) {
			scale = totalHeight / maxHeight;
		}

		matrices.push();

		final float totalWidthScaled;
		final float scaleX;
		if (maxWidth >= 0 && totalWidth > maxWidth * scale) {
			totalWidthScaled = maxWidth * scale;
			scaleX = totalWidth / maxWidth;
		} else {
			totalWidthScaled = totalWidth;
			scaleX = scale;
		}
		matrices.scale(1 / scaleX, 1 / scale, 1 / scale);

		float offset = verticalAlignment.getOffset(y * scale, totalHeight);
		for (int i = 0; i < orderedTexts.size(); i++) {
			final boolean isCJK = isCJKList.get(i);
			final int extraScale = isCJK ? 2 : 1;
			if (isCJK) {
				matrices.push();
				matrices.scale(extraScale, extraScale, 1);
			}

			final float xOffset = horizontalAlignment.getOffset(xAlignment.getOffset(x * scaleX, totalWidth), textRenderer.getWidth(orderedTexts.get(i)) * extraScale - totalWidth);

			final float shade = light == IGui.MAX_LIGHT_GLOWING ? 1 : Math.min(LightmapTextureManager.getBlockLightCoordinates(light) / 16F * 0.1F + 0.7F, 1);
			final int a = (textColor >> 24) & 0xFF;
			final int r = (int) (((textColor >> 16) & 0xFF) * shade);
			final int g = (int) (((textColor >> 8) & 0xFF) * shade);
			final int b = (int) ((textColor & 0xFF) * shade);

			textRenderer.draw(orderedTexts.get(i), xOffset / extraScale, offset / extraScale, (a << 24) + (r << 16) + (g << 8) + b, shadow, matrices.peek().getModel(), immediate, false, 0, light);

			if (isCJK) {
				matrices.pop();
			}

			offset += IGui.LINE_HEIGHT * extraScale;
		}

		matrices.pop();

		if (drawingCallback != null) {
			final float x1 = xAlignment.getOffset(x, totalWidthScaled / scale);
			final float y1 = verticalAlignment.getOffset(y, totalHeight / scale);
			drawingCallback.drawingCallback(x1, y1, x1 + totalWidthScaled / scale, y1 + totalHeight / scale);
		}
	}

	static void drawRectangle(VertexConsumer vertexConsumer, double x1, double y1, double x2, double y2, int color) {
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		vertexConsumer.vertex(x1, y1, 0).color(r, g, b, a).next();
		vertexConsumer.vertex(x1, y2, 0).color(r, g, b, a).next();
		vertexConsumer.vertex(x2, y2, 0).color(r, g, b, a).next();
		vertexConsumer.vertex(x2, y1, 0).color(r, g, b, a).next();
	}

	static void drawRailDebug(MatrixStack matrices, boolean s1, boolean s2, int xS, int zS, int xE, int zE, float xC1, float zC1, float r1, float xC2, float zC2, float r2, float yStart, float yEnd) {				
		final Matrix4f matrix4f = matrices.peek().getModel();

		/* //1.17
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		/*/ //1.16
		RenderSystem.shadeModel(7425);
		RenderSystem.enableAlphaTest();
		RenderSystem.defaultAlphaFunc();
		//*/

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.disableTexture();
		RenderSystem.disableBlend();
		RenderSystem.lineWidth(1.0F);

		final float yM = (yStart + yEnd) / 2;
		final float d = 0.5F;
		final float COS45 = (float) (1 / Math.sqrt(2));

		final float xO1 = (s1 ? xS : 0) + xC1 + d;
		final float zO1 = (s1 ? zS : 0) + zC1 + d;
		final float xO2 = (s2 ? xE : 0) + xC2 + d;
		final float zO2 = (s2 ? zE : 0) + zC2 + d;
		float cR1 = s1 ? 0.2F : 0.4F;
		float cG1 = 0.4F;
		float cB1 = s1 ? 0.4F : 0.2F;
		float cR2 = s2 ? 0.2F : 0.4F;
		float cG2 = 0.4F;
		float cB2 = s2 ? 0.4F : 0.2F;
		RenderSystem.disableDepthTest();

		/* //1.17
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
		/*/ //1.16
		bufferBuilder.begin(3, VertexFormats.POSITION_COLOR);
		//*/
		bufferBuilder.vertex(matrix4f, xS+d        , yM+0.1F, zS+d        ).color(cR1, cG1, cB1, 0F).next();
		bufferBuilder.vertex(matrix4f, xO1         , yM+0.1F, zO1         ).color(cR1, cG1, cB1, 1F).next();
		bufferBuilder.vertex(matrix4f, xO1         , yM+0.2F, zO1         ).color(cR1, cG1, cB1, 1F).next();
		if (s1) {
			bufferBuilder.vertex(matrix4f, xO1+zC1*r1  , yM+0.2F, zO1-xC1*r1  ).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1+zC1*r1  , yM+0.1F, zO1-xC1*r1  ).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1+zC1*r1  , yM+0.1F, zO1-xC1*r1  ).color(cR1, cG1, cB1, 0F).next();
		} else {
			bufferBuilder.vertex(matrix4f, xO1 + r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1 * COS45, yM + 0.2F, zO1 + r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1, yM + 0.2F, zO1 + r1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 - r1 * COS45, yM + 0.2F, zO1 + r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 - r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 - r1 * COS45, yM + 0.2F, zO1 - r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1, yM + 0.2F, zO1 - r1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1 * COS45, yM + 0.2F, zO1 - r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 0F).next();
		}
		
		bufferBuilder.vertex(matrix4f, xE+d        , yM-0.1F, zE+d      ).color(cR2, cG2, cB2, 0F).next();
		bufferBuilder.vertex(matrix4f, xO2         , yM-0.1F, zO2         ).color(cR2, cG2, cB2, 1F).next();
		bufferBuilder.vertex(matrix4f, xO2         , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
		if (s2) {
			bufferBuilder.vertex(matrix4f, xO2+zC2*r2  , yM-0.2F, zO2-xC2*r2  ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+zC2*r2  , yM-0.1F, zO2-xC2*r2  ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+zC2*r2  , yM-0.1F, zO2-xC2*r2  ).color(cR2, cG2, cB2, 0F).next();
		} else {
			bufferBuilder.vertex(matrix4f, xO2+r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2*COS45, yM-0.2F, zO2+r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2         , yM-0.2F, zO2+r2      ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2-r2*COS45, yM-0.2F, zO2+r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2-r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2-r2*COS45, yM-0.2F, zO2-r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2         , yM-0.2F, zO2-r2      ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2*COS45, yM-0.2F, zO2-r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 0F).next();
		}
		tessellator.draw();
		cR1 = s1 ? 0.5F : 1;
		cG1 = 1;
		cB1 = s1 ? 1 : 0.5F;
		cR2 = s2 ? 0.5F : 1;
		cG2 = 1;
		cB2 = s2 ? 1 : 0.5F;

		RenderSystem.enableDepthTest();

		/* //1.17
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
		/*/ //1.16
		bufferBuilder.begin(3, VertexFormats.POSITION_COLOR);
		//*/
		bufferBuilder.vertex(matrix4f, xS+d        , yM+0.1F, zS+d        ).color(1F, 0.5F, 0.5F, 0F).next();
		bufferBuilder.vertex(matrix4f, xS+d        , yM+0.1F, zS+d        ).color(1F, 0.5F, 0.5F, 0F).next();

		bufferBuilder.vertex(matrix4f, xS+d        , yM+0.1F, zS+d        ).color(cR1, cG1, cB1, 0F).next();
		bufferBuilder.vertex(matrix4f, xO1         , yM+0.1F, zO1         ).color(cR1, cG1, cB1, 1F).next();
		bufferBuilder.vertex(matrix4f, xO1         , yM+0.2F, zO1         ).color(cR1, cG1, cB1, 1F).next();
		if (s1) {
			bufferBuilder.vertex(matrix4f, xO1+zC1*r1  , yM+0.2F, zO1-xC1*r1  ).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1+zC1*r1  , yM+0.1F, zO1-xC1*r1  ).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1+zC1*r1  , yM+0.1F, zO1-xC1*r1  ).color(cR1, cG1, cB1, 0F).next();
		} else {
			bufferBuilder.vertex(matrix4f, xO1 + r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1 * COS45, yM + 0.2F, zO1 + r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1, yM + 0.2F, zO1 + r1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 - r1 * COS45, yM + 0.2F, zO1 + r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 - r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 - r1 * COS45, yM + 0.2F, zO1 - r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1, yM + 0.2F, zO1 - r1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1 * COS45, yM + 0.2F, zO1 - r1 * COS45).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 1F).next();
			bufferBuilder.vertex(matrix4f, xO1 + r1, yM + 0.2F, zO1).color(cR1, cG1, cB1, 0F).next();
		}
		
		bufferBuilder.vertex(matrix4f, xE+d        , yM-0.1F, zE+d      ).color(cR2, cG2, cB2, 0F).next();
		bufferBuilder.vertex(matrix4f, xO2         , yM-0.1F, zO2         ).color(cR2, cG2, cB2, 1F).next();
		bufferBuilder.vertex(matrix4f, xO2         , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
		if (s2) {
			bufferBuilder.vertex(matrix4f, xO2+zC2*r2  , yM-0.2F, zO2-xC2*r2  ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+zC2*r2  , yM-0.1F, zO2-xC2*r2  ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+zC2*r2  , yM-0.1F, zO2-xC2*r2  ).color(cR2, cG2, cB2, 0F).next();
		} else {
			bufferBuilder.vertex(matrix4f, xO2+r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2*COS45, yM-0.2F, zO2+r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2         , yM-0.2F, zO2+r2      ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2-r2*COS45, yM-0.2F, zO2+r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2-r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2-r2*COS45, yM-0.2F, zO2-r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2         , yM-0.2F, zO2-r2      ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2*COS45, yM-0.2F, zO2-r2*COS45).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 1F).next();
			bufferBuilder.vertex(matrix4f, xO2+r2      , yM-0.2F, zO2         ).color(cR2, cG2, cB2, 0F).next();
		}
		tessellator.draw();

		RenderSystem.lineWidth(1.0F);
		RenderSystem.enableBlend();
		RenderSystem.enableTexture();
	}

	static void drawTexture(MatrixStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x1, y1, z1, x2, y2, z2, 0, 0, 1, 1, facing, color, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumer vertexConsumer, float x, float y, float width, float height, Direction facing, int light) {
		drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, -1, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumer vertexConsumer, float x, float y, float width, float height, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x1, y2, z1, x2, y2, z2, x2, y1, z2, x1, y1, z1, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(MatrixStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		final Vec3i vec3i = facing.getVector();
		final Matrix4f matrix4f = matrices.peek().getModel();
		final Matrix3f matrix3f = matrices.peek().getNormal();
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		vertexConsumer.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x2, y2, z2).color(r, g, b, a).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
		vertexConsumer.vertex(matrix4f, x4, y4, z4).color(r, g, b, a).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, vec3i.getX(), vec3i.getY(), vec3i.getZ()).next();
	}

	static void setPositionAndWidth(ClickableWidget widget, int x, int y, int widgetWidth) {
		widget.x = x;
		widget.y = y;
		widget.setWidth(widgetWidth);
	}
}
