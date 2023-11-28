package io.peripage.domain;

/**
 * Specification parameters for each printer model. Required for unifying the
 * printing interface and easy adding new printer models.
 */
public enum PrinterType {
        A6(384),
        A6p( 576),
        A40(1728),
        A40p( 1848);

        /**
         * The max number of dots "pixels" the printer can print on a line.
         */
        private final int rowWidth;

        PrinterType(int rowWidth) {
            this.rowWidth = rowWidth;
        }

        /**
         * Row_bytes spec for current printer.
         *
         * Images are encoded as 1-pixel-per-1-bit, which means that 1 byte can
         * encode 8 black-white pixels in a line. This property defines the bytes
         * limit per image row, the overflow is truncated.
         */
        public int getRowBytes() {
            return rowWidth / 8 ;
        }

        /**
         * The max number of dots "pixels" the printer can print on a line.
         */
        public int getRowWidth() {
            return rowWidth;
        }

        /**
         * Get row_characters spec for current printer.
         * Internal ASCII printing mode allows printer to output the raw ASCII
         * letters up to `0x7f` and below to `0x10`. This property defines the
         * number of letters that can fit in a single row. in case of wrapped
         * printing, the overflow is wrapped using in-class buffer and synchronized
         * with the in-printer buffer.
         */
        public int getRowCharacters() {
            return rowWidth / 12;
        }
    }
