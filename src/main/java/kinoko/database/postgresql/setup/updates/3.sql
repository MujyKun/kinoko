-- 3.sql
-- Contains the schema for the Hired Merchant (Entrusted Shop) system.
-- This table stores items/mesos for Fredrick retrieval when a shop expires or closes.

BEGIN;

CREATE TABLE player.shop (
    id BIGSERIAL PRIMARY KEY,

    -- The character who owns this shop item
    character_id INT NOT NULL
        REFERENCES player.characters(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    -- Reference to the actual item (contains itemId, quantity, etc.)
    item_sn BIGINT
        REFERENCES item.items(item_sn)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    -- Shop listing details
    price INT NOT NULL DEFAULT 0,           -- Price per bundle
    bundles SMALLINT NOT NULL DEFAULT 1,    -- Number of bundles

    -- Sale status
    sold BOOLEAN NOT NULL DEFAULT FALSE,    -- false = unsold item, true = sold item

    -- Mesos earned from this sale (only populated when sold = true)
    mesos BIGINT NOT NULL DEFAULT 0,

    -- Buyer info (only populated when sold = true)
    buyer_name VARCHAR(13)
);

-- Index for quick lookups by character
CREATE INDEX idx_shop_character_id ON player.shop(character_id);

-- Index for finding unsold items
CREATE INDEX idx_shop_unsold ON player.shop(character_id, sold) WHERE sold = FALSE;

-- Index for finding sold items with mesos to collect
CREATE INDEX idx_shop_sold ON player.shop(character_id, sold) WHERE sold = TRUE;

COMMIT;
