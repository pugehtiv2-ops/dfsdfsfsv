package dev.pug;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Through-walls batch renderer: filled quads + camera-facing quad "lines", one draw call per frame. */
public final class Draw {
    private static final RenderPipeline PIPE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(PugClient.ID, "pipeline/through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    private static final ByteBufferBuilder ALLOC = new ByteBufferBuilder(1 << 20);
    private static final Vector4f COLOR_MOD = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFF = new Vector3f();
    private static final Matrix4f TEX = new Matrix4f();
    private static final int MAX_BOXES = 2500;

    private static MappableRingBuffer ring;
    private static BufferBuilder buf;
    private static Matrix4fc pose;
    private static Vec3 cam, look;
    private static float cr, cg, cb, ca;
    private static int boxes;

    private Draw() {}

    public static void init() {}

    public static Vec3 camera() { return cam; }

    public static boolean begin(WorldRenderContext c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || c.matrices() == null) return false;
        cam = c.worldState().cameraRenderState.pos;
        look = cam.add(mc.player.getViewVector(1.0f).scale(0.6));
        pose = c.matrices().last().pose();
        buf = null;
        boxes = 0;
        return true;
    }

    private static void color(int argb, float alphaMul) {
        ca = ((argb >>> 24) & 0xFF) / 255f * alphaMul;
        cr = ((argb >>> 16) & 0xFF) / 255f;
        cg = ((argb >>> 8) & 0xFF) / 255f;
        cb = (argb & 0xFF) / 255f;
    }

    private static void v(double x, double y, double z) {
        if (buf == null)
            buf = new BufferBuilder(ALLOC, PIPE.getVertexFormatMode(), PIPE.getVertexFormat());
        buf.addVertex(pose, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setColor(cr, cg, cb, ca);
    }

    private static void v(Vec3 p) { v(p.x, p.y, p.z); }

    public static void line(Vec3 a, Vec3 b, int argb) {
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 toCam = cam.subtract(mid);
        Vec3 side = b.subtract(a).cross(toCam);
        double len = side.length();
        if (len < 1.0e-6) return;
        double w = Math.max(0.01, Math.sqrt(toCam.lengthSqr()) * 0.0013);
        side = side.scale(w / len);
        Vec3 p1 = a.add(side), p2 = a.subtract(side), p3 = b.subtract(side), p4 = b.add(side);
        color(argb, 1f);
        v(p1); v(p2); v(p3); v(p4);
        v(p4); v(p3); v(p2); v(p1);
    }

    public static void tracer(Vec3 to, int argb) {
        line(look, to, argb);
    }

    public static void box(AABB b, int argb) {
        if (boxes++ >= MAX_BOXES) return;
        double x0 = b.minX, y0 = b.minY, z0 = b.minZ, x1 = b.maxX, y1 = b.maxY, z1 = b.maxZ;

        color(argb, 0.16f);
        v(x0, y0, z1); v(x1, y0, z1); v(x1, y1, z1); v(x0, y1, z1);
        v(x1, y0, z0); v(x0, y0, z0); v(x0, y1, z0); v(x1, y1, z0);
        v(x0, y0, z0); v(x0, y0, z1); v(x0, y1, z1); v(x0, y1, z0);
        v(x1, y0, z1); v(x1, y0, z0); v(x1, y1, z0); v(x1, y1, z1);
        v(x0, y1, z1); v(x1, y1, z1); v(x1, y1, z0); v(x0, y1, z0);
        v(x0, y0, z0); v(x1, y0, z0); v(x1, y0, z1); v(x0, y0, z1);

        Vec3[] p = {
                new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x0, y0, z1),
                new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1)
        };
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            line(p[i], p[j], argb);
            line(p[i + 4], p[j + 4], argb);
            line(p[i], p[i + 4], argb);
        }
    }

    public static void end() {
        if (buf == null) return;
        Minecraft client = Minecraft.getInstance();

        MeshData built = buf.buildOrThrow();
        MeshData.DrawState state = built.drawState();
        VertexFormat format = state.format();
        buf = null;

        int size = state.vertexCount() * format.getVertexSize();
        if (ring == null || ring.size() < size) {
            if (ring != null) ring.close();
            ring = new MappableRingBuffer(() -> PugClient.ID + " vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }

        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = enc.mapBuffer(
                ring.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(built.vertexBuffer(), view.data());
        }
        GpuBuffer vertices = ring.currentBuffer();

        built.sortQuads(ALLOC, RenderSystem.getProjectionType().vertexSorting());
        GpuBuffer indices = PIPE.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
        VertexFormat.IndexType indexType = built.drawState().indexType();

        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MOD, MODEL_OFF, TEX);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> PugClient.ID + " through walls",
                client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(PIPE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0, 0, state.indexCount(), 1);
        }

        built.close();
        ring.rotate();
    }
}
