// Proportional layout preview only. Not a release icon or a game asset.
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.IO.Compression;

public static class VoxelIconPreview
{
    private const int ImageSize = 1024;
    private const float UnitX = 25.5f;
    private const float UnitY = 12.75f;
    private const float UnitZ = 26f;
    private const float OriginX = ImageSize / 2f;
    private const float OriginY = 480f;

    private static readonly Dictionary<string, string> Blocks = new Dictionary<string, string>();
    private static readonly Dictionary<string, Bitmap> Textures = new Dictionary<string, Bitmap>();

    public static void Render(string minecraftJar, string outputPng)
    {
        Blocks.Clear();
        foreach (Bitmap bitmap in Textures.Values) bitmap.Dispose();
        Textures.Clear();

        using (ZipArchive archive = ZipFile.OpenRead(minecraftJar))
        {
            Load(archive, "grass_block_top");
            Load(archive, "grass_block_side");
            Load(archive, "dirt");
            Load(archive, "stone");
            Load(archive, "bedrock");
            Load(archive, "oak_log");
            Load(archive, "oak_log_top");
            Load(archive, "oak_leaves");
            Load(archive, "oak_planks");
            Load(archive, "cobblestone");
            Load(archive, "glass");
        }

        Tint("grass_block_top", Color.FromArgb(113, 183, 83));
        Tint("oak_leaves", Color.FromArgb(80, 161, 66));
        TintGrassSide();
        BuildScene();

        using (Bitmap canvas = new Bitmap(ImageSize, ImageSize, PixelFormat.Format32bppArgb))
        using (Graphics graphics = Graphics.FromImage(canvas))
        {
            graphics.Clear(Color.FromArgb(27, 34, 40));
            graphics.InterpolationMode = InterpolationMode.NearestNeighbor;
            graphics.SmoothingMode = SmoothingMode.None;
            graphics.PixelOffsetMode = PixelOffsetMode.Half;

            List<Voxel> voxels = new List<Voxel>();
            foreach (KeyValuePair<string, string> pair in Blocks)
            {
                string[] coords = pair.Key.Split(',');
                voxels.Add(new Voxel(int.Parse(coords[0]), int.Parse(coords[1]), int.Parse(coords[2]), pair.Value));
            }
            voxels.Sort((a, b) =>
            {
                int depth = (a.X + a.Z).CompareTo(b.X + b.Z);
                if (depth != 0) return depth;
                int height = a.Y.CompareTo(b.Y);
                return height != 0 ? height : a.X.CompareTo(b.X);
            });

            foreach (Voxel v in voxels)
            {
                if (!Has(v.X, v.Y, v.Z + 1)) DrawSideZ(graphics, v);
                if (!Has(v.X + 1, v.Y, v.Z)) DrawSideX(graphics, v);
                if (!Has(v.X, v.Y + 1, v.Z)) DrawTop(graphics, v);
            }
            canvas.Save(outputPng, ImageFormat.Png);
        }
        foreach (Bitmap bitmap in Textures.Values) bitmap.Dispose();
        Textures.Clear();
    }

    private static void BuildScene()
    {
        // Exactly 16 x 16 Minecraft blocks. Every tree and house dimension uses
        // the same 1-block grid as the terrain, unlike the generated sketches.
        for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++)
        for (int y = 0; y < 8; y++)
            Put(x, y, z, y == 7 ? "grass" : y >= 5 ? "dirt" : y == 0 ? "bedrock" : "stone");

        // Ordinary 1-block trunk and a restrained 5 x 5 oak canopy.
        for (int y = 8; y <= 12; y++) Put(2, y, 12, "log");
        for (int x = 0; x <= 4; x++)
        for (int z = 10; z <= 14; z++)
        {
            if ((x == 0 || x == 4) && (z == 10 || z == 14)) continue;
            Put(x, 12, z, "leaves");
            if (x >= 1 && x <= 3 && z >= 11 && z <= 13) Put(x, 13, z, "leaves");
        }
        Put(2, 14, 12, "leaves");

        // The visible half of a village house runs to the x = 16 cut. Its
        // planned other half would extend beyond this chunk. The full gable
        // width is retained so it still looks like a house at icon scale.
        for (int x = 10; x < 16; x++)
        for (int z = 6; z <= 11; z++) Put(x, 8, z, "cobblestone");

        for (int y = 9; y <= 11; y++)
        {
            for (int z = 6; z <= 11; z++) Put(10, y, z, z == 6 || z == 11 ? "log" : "planks");
            for (int x = 11; x < 16; x++)
            {
                Put(x, y, 6, "planks");
                Put(x, y, 11, (x == 12 && y == 10) ? "glass" : "planks");
            }
        }

        // Roof slopes are a symmetric 1:1 staircase over both sides of the
        // gable. The x-positive end remains visibly open at the chunk cut.
        for (int z = 6; z <= 11; z++)
        for (int y = 12; y < RoofY(z); y++) Put(10, y, z, "planks");
        for (int x = 9; x < 16; x++)
        for (int z = 5; z <= 12; z++) Put(x, RoofY(z), z, "planks");
    }

    private static int RoofY(int z)
    {
        return 11 + Math.Max(0, 3 - Math.Min(Math.Abs(z - 8), Math.Abs(z - 9)));
    }

    private static void DrawTop(Graphics g, Voxel v)
    {
        PointF a = Project(v.X, v.Y + 1, v.Z);
        PointF b = Project(v.X + 1, v.Y + 1, v.Z);
        PointF d = Project(v.X, v.Y + 1, v.Z + 1);
        DrawTexturedFace(g, a, b, d, Texture(v.Kind, true), 0);
    }

    private static void DrawSideX(Graphics g, Voxel v)
    {
        PointF a = Project(v.X + 1, v.Y + 1, v.Z);
        PointF b = Project(v.X + 1, v.Y + 1, v.Z + 1);
        PointF d = Project(v.X + 1, v.Y, v.Z);
        DrawTexturedFace(g, a, b, d, Texture(v.Kind, false), 32);
    }

    private static void DrawSideZ(Graphics g, Voxel v)
    {
        PointF a = Project(v.X, v.Y + 1, v.Z + 1);
        PointF b = Project(v.X + 1, v.Y + 1, v.Z + 1);
        PointF d = Project(v.X, v.Y, v.Z + 1);
        DrawTexturedFace(g, a, b, d, Texture(v.Kind, false), 16);
    }

    private static void DrawTexturedFace(Graphics g, PointF a, PointF b, PointF d, Bitmap texture, int shade)
    {
        PointF c = new PointF(b.X + d.X - a.X, b.Y + d.Y - a.Y);
        using (GraphicsPath clip = new GraphicsPath())
        using (Matrix transform = new Matrix(
            (b.X - a.X) / texture.Width, (b.Y - a.Y) / texture.Width,
            (d.X - a.X) / texture.Height, (d.Y - a.Y) / texture.Height,
            a.X, a.Y))
        {
            clip.AddPolygon(new[] { a, b, c, d });
            GraphicsState state = g.Save();
            g.SetClip(clip);
            g.MultiplyTransform(transform);
            g.DrawImage(texture, 0, 0, texture.Width, texture.Height);
            g.Restore(state);
            if (shade != 0)
            using (SolidBrush brush = new SolidBrush(Color.FromArgb(shade, 0, 0, 0)))
                g.FillPolygon(brush, new[] { a, b, c, d });
        }
    }

    private static PointF Project(float x, float y, float z)
    {
        return new PointF(OriginX + (x - z) * UnitX,
            OriginY + (x + z) * UnitY - y * UnitZ);
    }

    private static Bitmap Texture(string kind, bool top)
    {
        switch (kind)
        {
            case "grass": return Textures[top ? "grass_block_top" : "grass_block_side"];
            case "log": return Textures[top ? "oak_log_top" : "oak_log"];
            case "leaves": return Textures["oak_leaves"];
            case "planks": return Textures["oak_planks"];
            case "glass": return Textures["glass"];
            default: return Textures[kind];
        }
    }

    private static void Load(ZipArchive archive, string name)
    {
        ZipArchiveEntry entry = archive.GetEntry("assets/minecraft/textures/block/" + name + ".png");
        if (entry == null) throw new InvalidDataException("Missing Minecraft texture: " + name);
        using (Stream stream = entry.Open())
        using (Bitmap original = new Bitmap(stream))
            Textures[name] = new Bitmap(original);
    }

    private static void Tint(string name, Color color)
    {
        Bitmap bitmap = Textures[name];
        for (int y = 0; y < bitmap.Height; y++)
        for (int x = 0; x < bitmap.Width; x++)
        {
            Color source = bitmap.GetPixel(x, y);
            bitmap.SetPixel(x, y, Color.FromArgb(source.A,
                source.R * color.R / 255, source.G * color.G / 255, source.B * color.B / 255));
        }
    }

    private static void TintGrassSide()
    {
        Bitmap bitmap = Textures["grass_block_side"];
        for (int y = 0; y < Math.Min(4, bitmap.Height); y++)
        for (int x = 0; x < bitmap.Width; x++)
        {
            Color source = bitmap.GetPixel(x, y);
            if (source.R > source.B + 5) continue; // dirt pixels stay brown
            bitmap.SetPixel(x, y, Color.FromArgb(source.A,
                source.R * 113 / 255, source.G * 183 / 255, source.B * 83 / 255));
        }
    }

    private static string Key(int x, int y, int z) { return x + "," + y + "," + z; }
    private static bool Has(int x, int y, int z) { return Blocks.ContainsKey(Key(x, y, z)); }
    private static void Put(int x, int y, int z, string kind) { Blocks[Key(x, y, z)] = kind; }

    private sealed class Voxel
    {
        public readonly int X, Y, Z;
        public readonly string Kind;
        public Voxel(int x, int y, int z, string kind) { X = x; Y = y; Z = z; Kind = kind; }
    }
}
