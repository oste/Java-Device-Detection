/* *********************************************************************
 * This Source Code Form is copyright of 51Degrees Mobile Experts Limited. 
 * Copyright © 2015 51Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY
 * 
 * This Source Code Form is the subject of the following patent 
 * applications, owned by 51Degrees Mobile Experts Limited of 5 Charlotte
 * Close, Caversham, Reading, Berkshire, United Kingdom RG4 7BY: 
 * European Patent Application No. 13192291.6; and 
 * United States Patent Application Nos. 14/085,223 and 14/085,301.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.
 * 
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 * ********************************************************************* */
package fiftyone.mobile.detection.factories;

import fiftyone.mobile.detection.IReadonlyList;
import fiftyone.mobile.detection.cache.ICache;
import fiftyone.mobile.detection.cache.IPutCache;
import fiftyone.mobile.detection.cache.IValueLoader;
import fiftyone.mobile.detection.cache.LruCache;
import fiftyone.mobile.detection.entities.*;
import fiftyone.mobile.detection.entities.headers.Header;
import fiftyone.mobile.detection.entities.memory.MemoryFixedList;
import fiftyone.mobile.detection.entities.memory.PropertiesList;
import fiftyone.mobile.detection.entities.stream.StreamDataset;
import fiftyone.mobile.detection.entities.stream.IntegerList;
import fiftyone.mobile.detection.factories.stream.NodeStreamFactoryV31;
import fiftyone.mobile.detection.factories.stream.NodeStreamFactoryV32;
import fiftyone.mobile.detection.factories.stream.ProfileStreamFactory;
import fiftyone.mobile.detection.readers.BinaryReader;
import fiftyone.properties.CacheConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import static fiftyone.properties.CacheConstants.CacheType.*;
import static fiftyone.properties.CacheConstants.*;

/**
 * Factory class used to create a DetectorDataSet from a source data structure.
 * <p>
 * All the entities are held in the persistent store and only loads into memory
 * when required. A cache mechanism is used to improve efficiency as many
 * entities are frequently used in a high volume environment.
 * <p>
 * When using the create methods a default LRU caches are used. User supplied caches
 * may be used by creating with the {@link Builder} subclass.
 * <p>
 * The data set will be initialised very quickly as only the header information 
 * is loaded. Entities are then created when requested by the detection process 
 * and stored in a cache to avoid being recreated if their requested again after 
 * a short period of time.
 * <p>
 * The very small data structures RootNodes, Properties and Components are 
 * always stored in memory as there is no benefit retrieving them every time 
 * they're needed.
 * <p>
 * A dataset can be created in several ways:
 * <ul>
 *  <li>Using a data file:
 *  <p><code>Dataset ds = StreamFactory.create("path_to_file", false);</code>
 *  <p>Where the boolean flag indicates if the data file should or should not 
 *  be deleted when close() is invoked.
 *  <li>Using a byte array:
 *  <p><code>Dataset ds = StreamFactory.create(dataFileAsByteArray);</code>
 *  <p>Where the byte array is the 51Degrees device data file read into a byte
 *  array.
 *  <li>Using the Builder to supply caches:
 *  <p><code>Dataset ds = new StreamFactory.Builder()
 *          .addCache(cachetype, cache)
 *          .addCache(cachetype, cache)
 *          .build("path_to_file")</code>
 *  <p>Note that all caches must be specified when using the builder, the default
 *  no cache where none is specified for a particular {@link CacheType}.
 * </ul>
 */
public final class StreamFactory {

    /**
     * Constructor creates a new dataset from the supplied bytes array.
     * 
     * @param data a byte array containing the data file.
     * @return Stream Dataset object.
     * @throws IOException if there was a problem accessing data file.
     */
    public static StreamDataset create(byte[] data) throws IOException {
        StreamDataset dataSet = new StreamDataset(data, Modes.MEMORY_MAPPED);
        load(dataSet, new HashMap<CacheType, ICache>());
        return dataSet;
    }
    
    /**
     * Creates a new DataSet from the file provided. The last modified date of
     * the data set is the last write time of the data file provided.
     * @param filePath Uncompressed file containing the data for the data set.
     * @return A DataSet configured to read entities from the file path when
     *         required.
     * @throws IOException  if there was a problem accessing the data file.
     */
    public static StreamDataset create(String filePath)
            throws IOException {
        return create(filePath, false);
    }

    /**
     * Creates a new DataSet from the file provided. The last modified date of 
     * the data set is the last write time of the data file provided.
     * 
     * @param filePath Uncompressed file containing the data for the data set.
     * @param isTempFile True if the file should be deleted when the source is 
     *                   disposed
     * @return A DataSet configured to read entities from the file path when 
     *         required.
     * @throws IOException if there was a problem accessing data file.
     */
    public static StreamDataset create(String filePath, boolean isTempFile)
                                                            throws IOException {
        return create(filePath, 
                new Date(new File(filePath).lastModified()), 
                isTempFile);
    }

    /**
     * Constructor creates a new dataset from the supplied data file.
     * 
     * @param filepath name of the file (with path to file) to load data from.
     * @param lastModified Date and time the source data was last modified.
     * @param isTempFile True if the file should be deleted when the source is 
     * disposed.
     * @return Stream Dataset object.
     * @throws IOException if there was a problem accessing data file.
     */
    public static StreamDataset create(String filepath, Date lastModified,
            boolean isTempFile) throws IOException {
        StreamDataset dataSet =
                new StreamDataset(
                        filepath, 
                        lastModified, 
                        Modes.FILE, 
                        isTempFile);
        final java.util.Map<CacheType, ICache> cacheMap = new HashMap<CacheType, ICache>(5);
        cacheMap.put(StringsCache, new LruCache(STRINGS_CACHE_SIZE));
        cacheMap.put(NodesCache, new LruCache(NODES_CACHE_SIZE));
        cacheMap.put(ValuesCache, new LruCache(VALUES_CACHE_SIZE));
        cacheMap.put(ProfilesCache, new LruCache(PROFILES_CACHE_SIZE));
        cacheMap.put(SignaturesCache, new LruCache(SIGNATURES_CACHE_SIZE));
        load(dataSet, cacheMap);
        return dataSet;
    }

    public static class Builder {
        private java.util.Map<CacheType, ICache> cacheMap = new HashMap<CacheType, ICache>(5);
        boolean isTempFile = false;
        private static final Date DATE_NONE = new Date(0);
        private Date lastModified = DATE_NONE;


        public Builder addCache(CacheType cacheType, ICache cache) {
            cacheMap.put(cacheType, cache);
            return this;
        }

        public Builder isTempfile() {
            isTempFile = true;
            return this;
        }

        public Builder lastModified(Date date) {
            lastModified = date;
            return this;
        }

        public StreamDataset build(String filename) throws IOException {
            Date modDate = lastModified;
            if (modDate.equals(DATE_NONE)) {
                modDate = new Date(new File(filename).lastModified());
            }
            StreamDataset dataSet =
                    new StreamDataset(
                            filename, modDate,  Modes.FILE,  isTempFile);
            load(dataSet, cacheMap);
            return dataSet;
        }
    }


    /**
     * Class adapts an EntityFactory to a Loader
     *
     * @param <V> type of the entity
     */
    private static class EntityLoader<V> implements IValueLoader<Integer,V> {

        final StreamDataset dataset;
        final BaseEntityFactory<V> entityFactory;
        final Header header;
        boolean fixedLength = false;

        EntityLoader(Header header, StreamDataset dataset, BaseEntityFactory<V> entityFactory) {
            this.dataset = dataset;
            this.entityFactory = entityFactory;
            this.header = header;
            try {
                getEntityFactory().getLength();
                fixedLength = true;
            } catch (UnsupportedOperationException ignored) {
                // expected for variable length entities
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public V load(Integer key) throws IOException {
            BinaryReader reader = dataset.pool.getReader();
            try {
                if (fixedLength) {
                    reader.setPos(header.getStartPosition()
                            + (getEntityFactory().getLength() * key));
                } else {
                    reader.setPos(header.getStartPosition() + key);
                }
                return entityFactory.create(dataset, key, reader);
            } finally {
                dataset.pool.release(reader);
            }
        }

        @SuppressWarnings("WeakerAccess")
        public BaseEntityFactory<V> getEntityFactory() {
            return entityFactory;
        }

        public Header getHeader() {
            return header;
        }

    }

    /**
     * A cacheing entity loader that uses an {@link LruCache}
     *
     * @param <V> type of entity
     */
    private static class LruEntityLoader<V> extends EntityLoader<V> {

        private LruCache<Integer, V> cache;

        LruEntityLoader(final Header header, final StreamDataset dataset, final BaseEntityFactory<V> entityFactory, LruCache<Integer, V> cache) {
            super(header, dataset, entityFactory);
            this.cache = cache;
            this.cache.setCacheLoader(new IValueLoader<Integer, V>() {
                @Override
                public V load(Integer key) throws IOException {
                    // delegate to the enclosing class superclass method
                    return LruEntityLoader.super.load(key);
                }
            });
        }

        @Override
        public V load(Integer key) throws IOException {
            return cache.get(key);
        }
    }

    /**
     * A cacheing entity loader that uses a {@link IPutCache}
     *
     * @param <V> type of entity
     */

    private static class CachedEntityLoader<V> extends EntityLoader<V> {

        private IPutCache<Integer, V> cache;

        CachedEntityLoader(Header header, StreamDataset dataset, BaseEntityFactory<V> entityFactory, IPutCache<Integer, V> cache) {
            super(header, dataset, entityFactory);
            this.cache = cache;
        }

        @Override
        public V load(Integer key) throws IOException {
            V value;
            value = cache.get(key);
            if (value == null) {
                value = super.load(key);
                if (value != null) {
                    cache.put(key, value);
                }
            }
            return value;
        }
    }

    /**
     * Implementation of IReadOnlyList for Streams
     *
     * @param <T> type of entity
     */
    private static class StreamList <T extends BaseEntity> implements IReadonlyList <T> {

        private EntityLoader<T> loader;

        StreamList(EntityLoader<T> loader) {
            this.loader = loader;
        }

        @Override
        public T get(int i) throws IOException {
            return loader.load(i);
        }

        @Override
        public int size() {
            return this.loader.getHeader().getCount();
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                // the item number
                int count = 0;
                // the position in the file or the item number (as above)
                // depending on whether the entity is fixed or variable size
                int position = 0;

                @Override
                public boolean hasNext() {
                    return count < loader.getHeader().getCount();
                }

                 @Override
                public T next() {
                    try {
                        T result = get(position);
                        count ++;
                        if (loader.fixedLength) {
                            position ++;
                        } else {
                            // this method supported only for variable length entities
                            position += loader.getEntityFactory().getLength(result);
                        }
                        return result;
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove not supported");

                }
            };
        }
    }

    /**
     * helper to create an appropriate loader for a cached list given the cache type
     * @param header the header defining the list this will create the loader for
     * @param cache the cache, or null
     * @param dataset the dataset
     * @param factory the factory for the type
     * @param <T> the type
     * @return an entity loader
     */
    @SuppressWarnings("unchecked")
    private static <T> EntityLoader<T> getLoaderFor(Header header, ICache cache, StreamDataset dataset, BaseEntityFactory factory) {
        EntityLoader loader;
        if (cache == null) {
            loader = new EntityLoader(header, dataset, factory);
        } else if (cache instanceof LruCache) {
            loader = new LruEntityLoader(header, dataset, factory, (LruCache) cache);
        } else if (cache instanceof IPutCache) {
            loader = new CachedEntityLoader(header, dataset, factory, (IPutCache) cache);
        } else {
            throw new IllegalStateException("Cache must be null, LruCache or IPutCache");
        }
        return loader;

    }
    /**
     * Load the necessary values from the data
     * file in to the Dataset. Stream mode only loads the essential information 
     * such as file headers.
     * 
     * @param dataSet The dataset object to load in to.
     * @throws IOException if there was a problem accessing data file.
     */
    @SuppressWarnings("null")
    private static void load(StreamDataset dataSet, java.util.Map<CacheType, ICache> cacheMap) throws IOException {
        BinaryReader reader = dataSet.pool.getReader();
        try {
            dataSet.setCacheMap(cacheMap);
            reader.setPos(0);
            //Load headers that are common for both V31 and V32.
            CommonFactory.loadHeader(dataSet, reader);

            EntityLoader<AsciiString> loader = getLoaderFor(new Header(reader), cacheMap.get(StringsCache), dataSet, new AsciiStringFactory());
            dataSet.strings = new StreamList<AsciiString>(loader);
            
            MemoryFixedList<Component> components;
            switch (dataSet.versionEnum) {
                case PatternV31:
                    components = new MemoryFixedList<Component>(
                            dataSet, reader, new ComponentFactoryV31());
                    break;
                case PatternV32:
                    components = new MemoryFixedList<Component>(
                            dataSet, reader, new ComponentFactoryV32());
                    break;

                default: throw new IllegalStateException("Unknown data version number");
            }
            dataSet.components = components;
            
            MemoryFixedList<Map> maps = new MemoryFixedList<Map>(
                    dataSet, reader, new MapFactory());
            dataSet.maps = maps;
                      
            PropertiesList properties = new PropertiesList(
                    dataSet, reader, new PropertyFactory());
            dataSet.properties = properties;

            EntityLoader<Value> valueLoader = getLoaderFor(new Header(reader), cacheMap.get(ValuesCache),
                    dataSet, new ValueFactory());
            dataSet.values = new StreamList<Value>(valueLoader);

            EntityLoader<Profile> profileLoader = getLoaderFor(new Header(reader), cacheMap.get(ProfilesCache),
                    dataSet, new ProfileStreamFactory());
            dataSet.profiles = new StreamList<Profile>(profileLoader);

            switch (dataSet.versionEnum) {
                case PatternV31:
                    EntityLoader<Signature> signature31Loader = getLoaderFor(new Header(reader), cacheMap.get(SignaturesCache),
                            dataSet, new SignatureFactoryV31(dataSet));
                    dataSet.signatures = new StreamList<Signature>(signature31Loader);
                    break;
                case PatternV32:
                    EntityLoader<Signature> signature32Loader = getLoaderFor(new Header(reader), cacheMap.get(SignaturesCache),
                            dataSet, new SignatureFactoryV32(dataSet));
                    dataSet.signatures = new StreamList<Signature>(signature32Loader);
                    dataSet.signatureNodeOffsets = 
                            new IntegerList(dataSet, reader);
                    dataSet.nodeRankedSignatureIndexes = 
                            new IntegerList(dataSet, reader);
                    break;
            }
            dataSet.rankedSignatureIndexes = new IntegerList(dataSet, reader);
            
            switch (dataSet.versionEnum) {
                case PatternV31:
                    EntityLoader<Node> node31Loader = getLoaderFor(new Header(reader), cacheMap.get(NodesCache),
                        dataSet, new NodeStreamFactoryV31());
                    dataSet.nodes = new StreamList<Node>(node31Loader);
                    break;
                case PatternV32:
                    EntityLoader<Node> node32Loader = getLoaderFor(new Header(reader), cacheMap.get(NodesCache),
                            dataSet, new NodeStreamFactoryV32());
                    dataSet.nodes = new StreamList<Node>(node32Loader);
                    break;
            }
            
            MemoryFixedList<Node> rootNodes = new MemoryFixedList<Node>(
                    dataSet, reader, new RootNodeFactory());
            dataSet.rootNodes = rootNodes;
            
            MemoryFixedList<ProfileOffset> profileOffsets = 
                new MemoryFixedList<ProfileOffset>( dataSet, reader, 
                                                    new ProfileOffsetFactory());
             dataSet.profileOffsets = profileOffsets;
             
            //Read into memory all small lists which are frequently accessed.
            reader.setPos(components.header.getStartPosition());
            components.read(reader);
            reader.setPos(maps.header.getStartPosition());
            maps.read(reader);
            reader.setPos(properties.header.getStartPosition());
            properties.read(reader);
            reader.setPos(rootNodes.header.getStartPosition());
            rootNodes.read(reader);
            reader.setPos(profileOffsets.header.getStartPosition());
            profileOffsets.read(reader);
            
        } finally {
            if (reader != null) {
                dataSet.pool.release(reader);
            }
        }
    }
}
