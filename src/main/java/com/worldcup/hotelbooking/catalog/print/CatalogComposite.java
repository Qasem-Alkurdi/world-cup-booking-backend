package com.worldcup.hotelbooking.catalog.print;

public interface CatalogComposite extends CatalogComponent {
    void add(CatalogLeaf leaf);

    void remove(CatalogLeaf leaf);
}
