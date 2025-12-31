-- =====================================================
-- Test Data for Foxicon Store
-- Run this AFTER running schema.sql
-- =====================================================

-- =====================================================
-- 1. AVATARS (Can insert directly)
-- =====================================================

INSERT INTO avatars (
  name, slug, poster_url, thumbnail_url, price,
  description, short_description,
  poly_count, mat_count, mesh_count, texture_memory, download_size,
  blob_container_name, blob_file_path, blob_file_name,
  is_active, is_featured, category, platform
) VALUES
-- Featured avatars
(
  'Alette',
  'alette',
  'https://example.com/images/alette-poster.jpg',
  'https://example.com/images/alette-thumbnail.jpg',
  29.99,
  'A beautiful fox avatar with customizable expressions and multiple outfit options. Perfect for VRChat socializing.',
  'Cute fox avatar with multiple expressions',
  35000,
  45,
  12,
  '128 MB',
  '85 MB',
  'avatars',
  'avatars/alette/alette.vrca',
  'Alette_v1.2.vrca',
  true,
  true,
  'anime',
  'VRChat'
),
(
  'Azryth',
  'azryth',
  'https://example.com/images/azryth-poster.jpg',
  'https://example.com/images/azryth-thumbnail.jpg',
  24.99,
  'A sleek dragon avatar with animated wings and tail. Includes multiple color variants.',
  'Dragon avatar with animated wings',
  42000,
  52,
  15,
  '156 MB',
  '92 MB',
  'avatars',
  'avatars/azryth/azryth.vrca',
  'Azryth_Dragon_v2.0.vrca',
  true,
  true,
  'fantasy',
  'VRChat'
),
(
  'Noir',
  'noir',
  'https://example.com/images/noir-poster.jpg',
  'https://example.com/images/noir-thumbnail.jpg',
  34.99,
  'A mysterious dark-themed avatar with glowing effects and premium animations.',
  'Dark-themed avatar with glowing effects',
  38000,
  48,
  13,
  '142 MB',
  '88 MB',
  'avatars',
  'avatars/noir/noir.vrca',
  'Noir_Shadow_v1.5.vrca',
  true,
  true,
  'realistic',
  'VRChat'
),
(
  'Sadie',
  'sadie',
  'https://example.com/images/sadie-poster.jpg',
  'https://example.com/images/sadie-thumbnail.jpg',
  19.99,
  'A cheerful cat avatar perfect for beginners. Simple and optimized for performance.',
  'Beginner-friendly cat avatar',
  18000,
  28,
  8,
  '64 MB',
  '45 MB',
  'avatars',
  'avatars/sadie/sadie.vrca',
  'Sadie_Cat_v1.0.vrca',
  true,
  false,
  'anime',
  'VRChat'
),
(
  'Salem',
  'salem',
  'https://example.com/images/salem-poster.jpg',
  'https://example.com/images/salem-thumbnail.jpg',
  27.99,
  'A witch-themed avatar with magical particle effects and spell casting animations.',
  'Witch avatar with magical effects',
  32000,
  41,
  11,
  '118 MB',
  '76 MB',
  'avatars',
  'avatars/salem/salem.vrca',
  'Salem_Witch_v1.3.vrca',
  true,
  false,
  'fantasy',
  'VRChat'
),
(
  'Solstice',
  'solstice',
  'https://example.com/images/solstice-poster.jpg',
  'https://example.com/images/solstice-thumbnail.jpg',
  39.99,
  'Premium celestial-themed avatar with day/night cycle animations and constellation effects.',
  'Premium celestial avatar with day/night cycle',
  48000,
  58,
  16,
  '192 MB',
  '125 MB',
  'avatars',
  'avatars/solstice/solstice.vrca',
  'Solstice_Celestial_v2.1.vrca',
  true,
  true,
  'fantasy',
  'VRChat'
),
(
  'Luna',
  'luna',
  'https://example.com/images/luna-poster.jpg',
  'https://example.com/images/luna-thumbnail.jpg',
  22.99,
  'A moon-themed wolf avatar with howling animations and lunar glow effects.',
  'Moon-themed wolf avatar',
  29000,
  36,
  10,
  '98 MB',
  '62 MB',
  'avatars',
  'avatars/luna/luna.vrca',
  'Luna_Wolf_v1.4.vrca',
  true,
  false,
  'realistic',
  'VRChat'
),
(
  'Nova',
  'nova',
  'https://example.com/images/nova-poster.jpg',
  'https://example.com/images/nova-thumbnail.jpg',
  31.99,
  'A space-themed robot avatar with customizable LED patterns and futuristic animations.',
  'Space robot avatar with LED patterns',
  41000,
  50,
  14,
  '165 MB',
  '98 MB',
  'avatars',
  'avatars/nova/nova.vrca',
  'Nova_Robot_v1.8.vrca',
  true,
  false,
  'realistic',
  'VRChat'
),
(
  'Phoenix',
  'phoenix',
  'https://example.com/images/phoenix-poster.jpg',
  'https://example.com/images/phoenix-thumbnail.jpg',
  36.99,
  'A fire-themed phoenix avatar with flame particle effects and rebirth animations.',
  'Fire phoenix with flame effects',
  45000,
  55,
  17,
  '178 MB',
  '112 MB',
  'avatars',
  'avatars/phoenix/phoenix.vrca',
  'Phoenix_Fire_v2.2.vrca',
  true,
  true,
  'fantasy',
  'VRChat'
),
(
  'Zephyr',
  'zephyr',
  'https://example.com/images/zephyr-poster.jpg',
  'https://example.com/images/zephyr-thumbnail.jpg',
  26.99,
  'A wind-themed bird avatar with flowing animations and gust particle effects.',
  'Wind bird avatar with flowing animations',
  33000,
  42,
  12,
  '124 MB',
  '79 MB',
  'avatars',
  'avatars/zephyr/zephyr.vrca',
  'Zephyr_Wind_v1.6.vrca',
  true,
  false,
  'fantasy',
  'VRChat'
);

-- =====================================================
-- 2. USER PROFILES
-- NOTE: You need to create users in auth.users first!
-- Replace the UUIDs below with actual user IDs from auth.users
-- =====================================================

-- Example user profiles (replace UUIDs with real auth.users IDs)
-- To get real user IDs, first create users via Supabase Auth, then run:
-- SELECT id FROM auth.users;

-- Uncomment and modify these after creating users:
/*
INSERT INTO user_profiles (
  id, username, full_name, avatar_url, role,
  vrc_username, discord_username
) VALUES
(
  '00000000-0000-0000-0000-000000000001'::uuid, -- Replace with real user ID
  'admin_user',
  'Admin User',
  'https://example.com/avatars/admin.jpg',
  'admin',
  'AdminVR',
  'admin#1234'
),
(
  '00000000-0000-0000-0000-000000000002'::uuid, -- Replace with real user ID
  'customer1',
  'John Doe',
  'https://example.com/avatars/customer1.jpg',
  'customer',
  'JohnVR',
  'john#5678'
),
(
  '00000000-0000-0000-0000-000000000003'::uuid, -- Replace with real user ID
  'customer2',
  'Jane Smith',
  NULL,
  'customer',
  'JaneVR',
  NULL
);
*/

-- =====================================================
-- 3. ORDERS
-- NOTE: user_id can be NULL for guest orders
-- order_number will be auto-generated by trigger if not provided
-- =====================================================

-- Insert orders (order_number will be auto-generated)
-- These are example orders - adjust user_id to match your auth.users IDs

INSERT INTO orders (
  user_id, customer_email, customer_name,
  subtotal, tax_amount, total_amount, currency,
  stripe_payment_intent_id, stripe_session_id,
  payment_status, payment_method, status,
  paid_at, completed_at
) VALUES
-- Order 1: Completed order
(
  NULL, -- Guest order (or replace with real user_id UUID)
  'customer1@example.com',
  'John Doe',
  29.99,
  2.40,
  32.39,
  'USD',
  'pi_1234567890abcdef',
  'cs_1234567890abcdef',
  'succeeded',
  'card',
  'completed',
  NOW() - INTERVAL '5 days',
  NOW() - INTERVAL '5 days'
),
-- Order 2: Completed order (multi-item)
(
  NULL, -- Guest order (or replace with real user_id UUID)
  'customer2@example.com',
  'Jane Smith',
  64.98, -- 24.99 + 39.99
  5.20,
  70.18,
  'USD',
  'pi_abcdef1234567890',
  'cs_abcdef1234567890',
  'succeeded',
  'card',
  'completed',
  NOW() - INTERVAL '3 days',
  NOW() - INTERVAL '3 days'
),
-- Order 3: Pending order
(
  NULL, -- Guest order (or replace with real user_id UUID)
  'customer3@example.com',
  'Bob Johnson',
  34.99,
  2.80,
  37.79,
  'USD',
  NULL,
  'cs_pending123456789',
  'pending',
  NULL,
  'pending',
  NULL,
  NULL
),
-- Order 4: Processing order
(
  NULL, -- Guest order (or replace with real user_id UUID)
  'customer4@example.com',
  'Alice Williams',
  27.99,
  2.24,
  30.23,
  'USD',
  'pi_processing123456',
  'cs_processing123456',
  'processing',
  'card',
  'processing',
  NOW() - INTERVAL '1 hour',
  NULL
);

-- =====================================================
-- 4. ORDER ITEMS
-- Links to orders and avatars created above
-- =====================================================

INSERT INTO order_items (
  order_id, avatar_id, avatar_name, avatar_slug, price
) VALUES
-- Order 1 items (single item)
(
  (SELECT id FROM orders WHERE customer_email = 'customer1@example.com' LIMIT 1),
  (SELECT id FROM avatars WHERE slug = 'alette'),
  'Alette',
  'alette',
  29.99
),
-- Order 2 items (multi-item order)
(
  (SELECT id FROM orders WHERE customer_email = 'customer2@example.com' LIMIT 1),
  (SELECT id FROM avatars WHERE slug = 'azryth'),
  'Azryth',
  'azryth',
  24.99
),
(
  (SELECT id FROM orders WHERE customer_email = 'customer2@example.com' LIMIT 1),
  (SELECT id FROM avatars WHERE slug = 'solstice'),
  'Solstice',
  'solstice',
  39.99
),
-- Order 3 items
(
  (SELECT id FROM orders WHERE customer_email = 'customer3@example.com' LIMIT 1),
  (SELECT id FROM avatars WHERE slug = 'noir'),
  'Noir',
  'noir',
  34.99
),
-- Order 4 items
(
  (SELECT id FROM orders WHERE customer_email = 'customer4@example.com' LIMIT 1),
  (SELECT id FROM avatars WHERE slug = 'salem'),
  'Salem',
  'salem',
  27.99
);

-- =====================================================
-- 5. PURCHASES
-- Links users to avatars they own
-- NOTE: Requires real user_id from auth.users
-- =====================================================

-- These are example purchases - uncomment and modify after creating users
/*
INSERT INTO purchases (
  user_id, avatar_id, order_id, order_item_id, amount_paid
) VALUES
-- User 1 purchases
(
  '00000000-0000-0000-0000-000000000002'::uuid, -- Replace with real user ID
  (SELECT id FROM avatars WHERE slug = 'alette'),
  (SELECT id FROM orders WHERE customer_email = 'customer1@example.com' LIMIT 1),
  (SELECT id FROM order_items WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'alette') LIMIT 1),
  29.99
),
-- User 2 purchases (multi-item order)
(
  '00000000-0000-0000-0000-000000000003'::uuid, -- Replace with real user ID
  (SELECT id FROM avatars WHERE slug = 'azryth'),
  (SELECT id FROM orders WHERE customer_email = 'customer2@example.com' LIMIT 1),
  (SELECT id FROM order_items WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'azryth') LIMIT 1),
  24.99
),
(
  '00000000-0000-0000-0000-000000000003'::uuid, -- Replace with real user ID
  (SELECT id FROM avatars WHERE slug = 'solstice'),
  (SELECT id FROM orders WHERE customer_email = 'customer2@example.com' LIMIT 1),
  (SELECT id FROM order_items WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'solstice') LIMIT 1),
  39.99
);
*/

-- =====================================================
-- 6. DOWNLOADS
-- Download links for purchased avatars
-- NOTE: Requires real user_id and purchase_id
-- =====================================================

-- These are example downloads - uncomment and modify after creating purchases
/*
INSERT INTO downloads (
  user_id, avatar_id, order_id, order_item_id, purchase_id,
  download_url, expires_at, is_used, used_at, access_count,
  downloaded_from_ip, user_agent
) VALUES
-- Download 1: Used download
(
  '00000000-0000-0000-0000-000000000002'::uuid, -- Replace with real user ID
  (SELECT id FROM avatars WHERE slug = 'alette'),
  (SELECT id FROM orders WHERE customer_email = 'customer1@example.com' LIMIT 1),
  (SELECT id FROM order_items WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'alette') LIMIT 1),
  (SELECT id FROM purchases WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'alette') LIMIT 1),
  'https://storage.azure.com/avatars/alette/download?sv=2023-01-03&sig=...',
  NOW() + INTERVAL '48 hours',
  true,
  NOW() - INTERVAL '2 days',
  1,
  '192.168.1.100',
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
),
-- Download 2: Active download (not used yet)
(
  '00000000-0000-0000-0000-000000000003'::uuid, -- Replace with real user ID
  (SELECT id FROM avatars WHERE slug = 'azryth'),
  (SELECT id FROM orders WHERE customer_email = 'customer2@example.com' LIMIT 1),
  (SELECT id FROM order_items WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'azryth') LIMIT 1),
  (SELECT id FROM purchases WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'azryth') LIMIT 1),
  'https://storage.azure.com/avatars/azryth/download?sv=2023-01-03&sig=...',
  NOW() + INTERVAL '24 hours',
  false,
  NULL,
  0,
  NULL,
  NULL
),
-- Download 3: Expired download
(
  '00000000-0000-0000-0000-000000000003'::uuid, -- Replace with real user ID
  (SELECT id FROM avatars WHERE slug = 'solstice'),
  (SELECT id FROM orders WHERE customer_email = 'customer2@example.com' LIMIT 1),
  (SELECT id FROM order_items WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'solstice') LIMIT 1),
  (SELECT id FROM purchases WHERE avatar_id = (SELECT id FROM avatars WHERE slug = 'solstice') LIMIT 1),
  'https://storage.azure.com/avatars/solstice/download?sv=2023-01-03&sig=...',
  NOW() - INTERVAL '1 day', -- Expired
  false,
  NULL,
  0,
  NULL,
  NULL
);
*/

-- =====================================================
-- VERIFICATION QUERIES
-- Run these to verify the test data
-- =====================================================

-- Count records in each table
SELECT 'avatars' as table_name, COUNT(*) as count FROM avatars
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;
-- UNION ALL
-- SELECT 'user_profiles', COUNT(*) FROM user_profiles
-- UNION ALL
-- SELECT 'purchases', COUNT(*) FROM purchases
-- UNION ALL
-- SELECT 'downloads', COUNT(*) FROM downloads;

-- View all avatars
SELECT id, name, slug, price, is_active, is_featured, category
FROM avatars
ORDER BY created_at DESC;

-- View all orders with items
SELECT 
  o.order_number,
  o.customer_email,
  o.total_amount,
  o.status,
  o.payment_status,
  oi.avatar_name,
  oi.price as item_price
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
ORDER BY o.created_at DESC;

-- View featured avatars
SELECT name, slug, price, category, short_description
FROM avatars
WHERE is_featured = true
ORDER BY price DESC;

-- View active avatars by category
SELECT category, COUNT(*) as count, AVG(price) as avg_price
FROM avatars
WHERE is_active = true
GROUP BY category
ORDER BY count DESC;

