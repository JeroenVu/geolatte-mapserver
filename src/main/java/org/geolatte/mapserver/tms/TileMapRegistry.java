/*
 * Copyright 2009-2010  Geovise BVBA, QMINO BVBA
 *
 * This file is part of GeoLatte Mapserver.
 *
 * GeoLatte Mapserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GeoLatte Mapserver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GeoLatte Mapserver.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.geolatte.mapserver.tms;

import org.apache.log4j.Logger;
import org.geolatte.mapserver.config.Configuration;
import org.geolatte.mapserver.config.ConfigurationException;

import java.util.*;

public class TileMapRegistry {

    private final static Logger LOGGER = Logger.getLogger(TileMapRegistry.class);

    private final Map<String, TileMap> tileMaps;

    public static TileMapRegistry configure(Configuration config) {
        Map<String, TileMap> map = new HashMap<String, TileMap>();
        for (String tilemap : config.getTileMaps()) {
            addTilemap(map, tilemap, config);
        }
        return new TileMapRegistry(map);
    }

    private TileMapRegistry(Map<String, TileMap> map) {
        this.tileMaps = Collections.unmodifiableMap(map);
    }

    private static void addTilemap(Map<String, TileMap> map, String tileMapName, Configuration config) {
        try {
            TileMap tilemap = createTileMap(tileMapName, config);
            map.put(tilemap.getTitle(), tilemap);
        } catch (TileMapCreationException e) {
            LOGGER.warn(String.format("Failed to instantiate TileMap \"%s\": %s", tileMapName, e.getMessage()));
        }
    }

    private static TileMap createTileMap(String tileMapName, Configuration config) throws TileMapCreationException {
        String sourceFactoryName = null;
        try {
            sourceFactoryName = config.getTileImageSourceFactoryClass(tileMapName);
            String path = config.getPath(tileMapName);
            Configuration.RESOURCE_TYPE type = config.getType(tileMapName);
            return createTileMap(sourceFactoryName, path, type);
        } catch (ConfigurationException e) {
            throw new TileMapCreationException("Cannot create tilemap: " + tileMapName, e);
        }
    }

    private static TileMap createTileMap(String sourceFactoryName, String path, Configuration.RESOURCE_TYPE type) throws TileMapCreationException {
        try {
            Class factClass = Class.forName(sourceFactoryName);
            TileImageSourceFactory factory = (TileImageSourceFactory) factClass.newInstance();
            TileMapBuilder builder = null;
            switch (type) {
                case FILE:
                    builder = TileMapBuilder.fromPath(path);
                    break;
                case URL:
                    builder = TileMapBuilder.fromURL(path);
                    break;
                default:
                    throw new IllegalStateException();
            }
            return builder.buildTileMap(factory);
        } catch (ClassNotFoundException e) {
            throw new TileMapCreationException(String.format("Can't locate source factory %s.", sourceFactoryName), e);
        } catch (IllegalAccessException e) {
            throw new TileMapCreationException(String.format("Can't instantiate source factory %s.", sourceFactoryName), e);
        } catch (InstantiationException e) {
            throw new TileMapCreationException(String.format("Can't instantiate source factory %s.", sourceFactoryName), e);
        } catch (ClassCastException e) {
            throw new TileMapCreationException(String.format("Configured source factory %s. is not a TileImageSourceFactory implementation", sourceFactoryName), e);
        }
    }

    public List<String> getTileMapNames() {
        Set<String> set = tileMaps.keySet();
        List<String> result = new ArrayList<String>();
        result.addAll(set);
        return result;
    }

    public TileMap getTileMap(String tileMapName) {
        return this.tileMaps.get(tileMapName);
    }
}