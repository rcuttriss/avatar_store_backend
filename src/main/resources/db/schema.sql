-- =====================================================
-- Complete Database Schema
-- Ecommerce Avatar Store with Azure Blob Storage
-- =====================================================

-- =====================================================
-- 1. AVATARS TABLE
-- Stores avatar product information
-- =====================================================
CREATE TABLE IF NOT EXISTS avatars (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  slug TEXT UNIQUE NOT NULL, -- URL-friendly identifier
  poster_url TEXT NOT NULL, -- Thumbnail/poster image URL
  thumbnail_url TEXT NOT NULL, -- Small thumbnail URL
  
  -- Pricing
  price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
  
  -- Description and metadata
  description TEXT,
  short_description TEXT, -- Brief description for listings
  
  -- Technical specifications
  poly_count INTEGER,
  mat_count INTEGER,
  mesh_count INTEGER,
  texture_memory TEXT,
  download_size TEXT,
  
  -- Azure Blob Storage paths
  -- Store the blob path/container reference for the avatar file
  blob_container_name TEXT DEFAULT 'avatars', -- Container name in Azure
  blob_file_path TEXT NOT NULL, -- Path to the .vrca or .unitypackage file in blob storage
  blob_file_name TEXT NOT NULL, -- Original filename
  
  -- Status and visibility
  is_active BOOLEAN DEFAULT true, -- Whether avatar is available for purchase
  is_featured BOOLEAN DEFAULT false, -- Featured on homepage
  
  -- Categories/Tags (can be extended with separate table if needed)
  category TEXT, -- e.g., 'realistic', 'anime', 'fantasy'
  platform TEXT DEFAULT 'VRChat', -- Platform (VRChat, Neos, etc.)
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 2. USER PROFILES TABLE
-- Extended user information (Supabase auth.users is separate)
-- =====================================================
CREATE TABLE IF NOT EXISTS user_profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  -- Note: email is available in auth.users table via id reference
  username TEXT UNIQUE,
  full_name TEXT,
  avatar_url TEXT, -- Profile picture URL
  
  -- Role management
  role TEXT DEFAULT 'customer' CHECK (role IN ('customer', 'admin')),
  
  -- Optional preferences
  vrc_username TEXT, -- VRChat username
  discord_username TEXT,
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- NOTE: Cart is handled client-side with localStorage/cookies
-- Cart items are sent directly to checkout API when creating orders
-- =====================================================

-- =====================================================
-- 3. ORDERS TABLE
-- Completed orders with payment information
-- =====================================================
CREATE TABLE IF NOT EXISTS orders (
  id BIGSERIAL PRIMARY KEY,
  order_number TEXT UNIQUE NOT NULL, -- Human-readable order number (e.g., ORD-2024-001)
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  
  -- Customer information (snapshot at time of purchase)
  customer_email TEXT NOT NULL,
  customer_name TEXT,
  
  -- Pricing
  subtotal NUMERIC(10, 2) NOT NULL,
  tax_amount NUMERIC(10, 2) DEFAULT 0.00,
  total_amount NUMERIC(10, 2) NOT NULL,
  currency TEXT DEFAULT 'USD',
  
  -- Payment information
  stripe_payment_intent_id TEXT UNIQUE, -- Stripe PaymentIntent ID
  stripe_session_id TEXT, -- Stripe Checkout Session ID
  payment_status TEXT DEFAULT 'pending' CHECK (payment_status IN ('pending', 'processing', 'succeeded', 'failed', 'refunded', 'cancelled')),
  payment_method TEXT, -- e.g., 'card', 'paypal'
  
  -- Order status
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'processing', 'completed', 'cancelled', 'refunded')),
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  paid_at TIMESTAMPTZ, -- When payment was confirmed
  completed_at TIMESTAMPTZ -- When order was fulfilled
);

-- =====================================================
-- 4. ORDER ITEMS TABLE
-- Items within each order (for support, refunds, order history)
-- =====================================================
CREATE TABLE IF NOT EXISTS order_items (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
  avatar_id BIGINT REFERENCES avatars(id) ON DELETE RESTRICT, -- Don't delete if avatar still exists
  
  -- Product snapshot (store details at time of purchase for historical accuracy)
  avatar_name TEXT NOT NULL, -- Snapshot of avatar name at purchase time
  avatar_slug TEXT NOT NULL, -- Snapshot of avatar slug at purchase time
  price NUMERIC(10, 2) NOT NULL, -- Price at time of purchase
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 5. PURCHASES TABLE
-- Links users to their purchased avatars (for quick ownership checks)
-- Note: This tracks ownership, while order_items tracks order history
-- =====================================================
CREATE TABLE IF NOT EXISTS purchases (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  avatar_id BIGINT REFERENCES avatars(id) ON DELETE CASCADE,
  order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
  order_item_id BIGINT REFERENCES order_items(id) ON DELETE SET NULL, -- Link to order item for reference
  
  -- Purchase details
  amount_paid NUMERIC(10, 2) NOT NULL, -- Price at time of purchase (snapshot)
  
  -- Optional: Product snapshot (in case avatar name/slug changes later)
  -- avatar_name TEXT, -- Snapshot of avatar name at purchase time
  -- avatar_slug TEXT, -- Snapshot of avatar slug at purchase time
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  
  -- Ensure unique purchase per user per avatar
  UNIQUE(user_id, avatar_id)
);

-- =====================================================
-- 6. DOWNLOADS TABLE
-- Track download history and manage download links
-- =====================================================
CREATE TABLE IF NOT EXISTS downloads (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  avatar_id BIGINT REFERENCES avatars(id) ON DELETE CASCADE,
  order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
  order_item_id BIGINT REFERENCES order_items(id) ON DELETE SET NULL,
  purchase_id BIGINT REFERENCES purchases(id) ON DELETE SET NULL,
  
  -- Download link information
  download_url TEXT NOT NULL, -- SAS URL or secure download link
  -- Note: blob_path and file_name can be retrieved from avatars table via avatar_id
  -- This avoids data duplication and ensures consistency if avatar file paths change
  
  -- Security and access control
  expires_at TIMESTAMPTZ NOT NULL, -- When the download link expires (typically 24-48 hours)
  is_used BOOLEAN DEFAULT false, -- Whether the download link has been used
  used_at TIMESTAMPTZ, -- When the download was accessed
  access_count INTEGER DEFAULT 0, -- Number of times accessed
  
  -- IP and user agent tracking (optional, for analytics)
  downloaded_from_ip TEXT,
  user_agent TEXT,
  
  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 6. INDEXES for Performance
-- =====================================================

-- Avatars indexes
CREATE INDEX IF NOT EXISTS idx_avatars_slug ON avatars(slug);
CREATE INDEX IF NOT EXISTS idx_avatars_price ON avatars(price);
CREATE INDEX IF NOT EXISTS idx_avatars_is_active ON avatars(is_active);
CREATE INDEX IF NOT EXISTS idx_avatars_is_featured ON avatars(is_featured);
CREATE INDEX IF NOT EXISTS idx_avatars_category ON avatars(category);
CREATE INDEX IF NOT EXISTS idx_avatars_created_at ON avatars(created_at DESC);

-- Orders indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_stripe_payment_intent_id ON orders(stripe_payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);

-- Order items indexes
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_avatar_id ON order_items(avatar_id);

-- Purchases indexes
CREATE INDEX IF NOT EXISTS idx_purchases_user_id ON purchases(user_id);
CREATE INDEX IF NOT EXISTS idx_purchases_avatar_id ON purchases(avatar_id);
CREATE INDEX IF NOT EXISTS idx_purchases_order_id ON purchases(order_id);
CREATE INDEX IF NOT EXISTS idx_purchases_order_item_id ON purchases(order_item_id);

-- Downloads indexes
CREATE INDEX IF NOT EXISTS idx_downloads_user_id ON downloads(user_id);
CREATE INDEX IF NOT EXISTS idx_downloads_avatar_id ON downloads(avatar_id);
CREATE INDEX IF NOT EXISTS idx_downloads_order_id ON downloads(order_id);
CREATE INDEX IF NOT EXISTS idx_downloads_order_item_id ON downloads(order_item_id);
CREATE INDEX IF NOT EXISTS idx_downloads_expires_at ON downloads(expires_at);
CREATE INDEX IF NOT EXISTS idx_downloads_created_at ON downloads(created_at DESC);

-- User profiles indexes
-- Note: email is in auth.users table, not user_profiles, so no index needed here
CREATE INDEX IF NOT EXISTS idx_user_profiles_username ON user_profiles(username);
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles(role);

-- =====================================================
-- 7. FUNCTIONS and TRIGGERS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to generate order number (trigger function)
CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS TRIGGER AS $$
DECLARE
  order_num TEXT;
  year_part TEXT;
  seq_num INTEGER;
BEGIN
  -- Only generate if order_number is not provided
  IF NEW.order_number IS NULL OR NEW.order_number = '' THEN
    year_part := TO_CHAR(NOW(), 'YYYY');
    
    -- Get the next sequence number for this year
    SELECT COALESCE(MAX(CAST(SUBSTRING(order_number FROM '(\d+)$') AS INTEGER)), 0) + 1
    INTO seq_num
    FROM orders
    WHERE order_number LIKE 'ORD-' || year_part || '-%';
    
    order_num := 'ORD-' || year_part || '-' || LPAD(seq_num::TEXT, 6, '0');
    NEW.order_number := order_num;
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to check if current user is admin (security definer to bypass RLS)
CREATE OR REPLACE FUNCTION is_admin()
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM user_profiles
    WHERE id = auth.uid() AND role = 'admin'
  );
END;
$$;

-- Trigger to auto-generate order number
CREATE TRIGGER generate_order_number_trigger
  BEFORE INSERT ON orders
  FOR EACH ROW
  EXECUTE FUNCTION generate_order_number();

-- Triggers to auto-update updated_at
CREATE TRIGGER update_avatars_updated_at BEFORE UPDATE ON avatars
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_downloads_updated_at BEFORE UPDATE ON downloads
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 8. ROW LEVEL SECURITY (RLS) POLICIES
-- =====================================================

-- Enable RLS on all tables
ALTER TABLE avatars ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchases ENABLE ROW LEVEL SECURITY;
ALTER TABLE downloads ENABLE ROW LEVEL SECURITY;

-- Avatars policies
CREATE POLICY "Avatars are viewable by everyone"
  ON avatars FOR SELECT
  USING (true);

CREATE POLICY "Admins can manage avatars"
  ON avatars FOR ALL
  USING (is_admin());

-- User profiles policies
CREATE POLICY "Users can view their own profile"
  ON user_profiles FOR SELECT
  USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
  ON user_profiles FOR UPDATE
  USING (auth.uid() = id);

CREATE POLICY "Users can insert their own profile"
  ON user_profiles FOR INSERT
  WITH CHECK (auth.uid() = id);

CREATE POLICY "Admins can view all profiles"
  ON user_profiles FOR SELECT
  USING (is_admin());

-- Orders policies
CREATE POLICY "Users can view their own orders"
  ON orders FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Admins can view all orders"
  ON orders FOR SELECT
  USING (is_admin());

-- Order items policies (same access as orders)
CREATE POLICY "Users can view items from their orders"
  ON order_items FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM orders
      WHERE orders.id = order_items.order_id AND orders.user_id = auth.uid()
    )
  );

CREATE POLICY "Admins can view all order items"
  ON order_items FOR SELECT
  USING (is_admin());

-- Purchases policies
CREATE POLICY "Users can view their own purchases"
  ON purchases FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Admins can view all purchases"
  ON purchases FOR SELECT
  USING (is_admin());

-- Downloads policies
CREATE POLICY "Users can view their own downloads"
  ON downloads FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own downloads"
  ON downloads FOR UPDATE
  USING (auth.uid() = user_id);

CREATE POLICY "Admins can view all downloads"
  ON downloads FOR SELECT
  USING (is_admin());

-- =====================================================
-- 9. HELPER VIEWS (Optional but useful)
-- =====================================================

-- View: User purchase summary
CREATE OR REPLACE VIEW user_purchase_summary AS
SELECT 
  p.user_id,
  COUNT(DISTINCT p.avatar_id) as total_avatars_purchased,
  COUNT(DISTINCT p.order_id) as total_orders,
  SUM(p.amount_paid) as total_spent,
  MAX(p.created_at) as last_purchase_date
FROM purchases p
GROUP BY p.user_id;

-- View: Avatar sales summary
CREATE OR REPLACE VIEW avatar_sales_summary AS
SELECT 
  a.id as avatar_id,
  a.name as avatar_name,
  COUNT(DISTINCT p.user_id) as total_purchasers,
  COUNT(p.id) as total_sales,
  SUM(p.amount_paid) as total_revenue,
  AVG(p.amount_paid) as average_sale_price
FROM avatars a
LEFT JOIN purchases p ON a.id = p.avatar_id
GROUP BY a.id, a.name;

-- =====================================================
-- END OF SCHEMA
-- =====================================================
