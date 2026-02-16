
-- Seed system categories
INSERT INTO categories (id, name, icon, color, is_system, sort_order) VALUES
      (gen_random_uuid(), 'Food & Drink', 'restaurant', '#FF6B6B', TRUE, 1),
      (gen_random_uuid(), 'Transportation', 'directions_car', '#4ECDC4', TRUE, 2),
      (gen_random_uuid(), 'Entertainment', 'movie', '#45B7D1', TRUE, 3),
      (gen_random_uuid(), 'Shopping', 'shopping_bag', '#96CEB4', TRUE, 4),
      (gen_random_uuid(), 'Utilities', 'bolt', '#FFEAA7', TRUE, 5),
      (gen_random_uuid(), 'Rent', 'home', '#DDA0DD', TRUE, 6),
      (gen_random_uuid(), 'Travel', 'flight', '#98D8C8', TRUE, 7),
      (gen_random_uuid(), 'Healthcare', 'local_hospital', '#F7DC6F', TRUE, 8),
      (gen_random_uuid(), 'Groceries', 'local_grocery_store', '#82E0AA', TRUE, 9),
      (gen_random_uuid(), 'Other', 'more_horiz', '#BDC3C7', TRUE, 100);