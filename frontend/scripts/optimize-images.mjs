import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const ASSETS_DIR = path.join(__dirname, '../src/assets/images');
const PUBLIC_DIR = path.join(__dirname, '../public/assets/images');

// Note: To use this script, ensure 'sharp' is installed: `npm i -D sharp`
// This script recursively finds .png and .jpg files, converts them to .webp using sharp,
// and saves them to the public/assets directory to be served statically.

async function optimizeImages() {
  try {
    const sharp = (await import('sharp')).default;
    console.log('🔄 Starting full image optimization pipeline...');

    async function processDirectory(dir) {
      if (!fs.existsSync(dir)) return;
      const entries = fs.readdirSync(dir, { withFileTypes: true });

      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        
        if (entry.isDirectory()) {
          await processDirectory(fullPath);
        } else if (/\.(png|jpe?g)$/i.test(entry.name)) {
          const relativePath = path.relative(ASSETS_DIR, dir);
          const outDir = path.join(PUBLIC_DIR, relativePath);
          
          if (!fs.existsSync(outDir)) {
            fs.mkdirSync(outDir, { recursive: true });
          }

          const outFile = path.join(outDir, entry.name.replace(/\.(png|jpe?g)$/i, '.webp'));
          
          await sharp(fullPath)
            .webp({ quality: 80, effort: 6 }) // High effort compression for pristine quality
            .toFile(outFile);
            
          console.log(`✅ Optimized: ${entry.name} -> .webp`);
        }
      }
    }

    await processDirectory(ASSETS_DIR);
    console.log('✨ Image pipeline completed. WebP assets ready in public/assets/images/');
  } catch (err) {
    if (err.code === 'ERR_MODULE_NOT_FOUND') {
      console.error('❌ Skipped image optimization: "sharp" module not found. Run `npm i -D sharp` to enable this pipeline.');
    } else {
      console.error('❌ Image optimization failed:', err);
    }
  }
}

optimizeImages();
