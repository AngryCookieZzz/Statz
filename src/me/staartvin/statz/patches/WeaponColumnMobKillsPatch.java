package me.staartvin.statz.patches;

import me.staartvin.statz.Statz;
import me.staartvin.statz.datamanager.player.PlayerStat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class WeaponColumnMobKillsPatch extends Patch {

    public WeaponColumnMobKillsPatch(Statz plugin) {
        super(plugin);
    }

    @Override
    public boolean applyMySQLChanges() {

        String tableName = this.getDatabaseConnector().getTable(PlayerStat.KILLS_MOBS).getTableName();

        List<String> queries = Arrays.asList("ALTER TABLE " + tableName + " ADD weapon VARCHAR(255) NOT NULL", "ALTER" +
                " TABLE " + tableName
                + " DROP INDEX `uuid`, ADD UNIQUE `uuid` (`uuid`, `mob`, `world`, `weapon`) USING BTREE;");

        try {
            this.getDatabaseConnector().sendQueries(queries, false);

            return true;

        } catch (Exception e) {

            this.getStatz().getLogger().warning("Failed to patch MySQL database for patch " + this.getPatchId());
            return false;
        }

    }

    @Override
    public String getPatchName() {
        return "Weapon Column - Mob Kills";
    }

    @Override
    public int getPatchId() {
        return 1;
    }

    @Override
    public boolean isPatchNeeded() {

        String query;
        String tableName = this.getDatabaseConnector().getTable(PlayerStat.KILLS_MOBS).getTableName();

        // The query is different whether it's MySQL or SQLite
        if (this.plugin.getConfigHandler().isMySQLEnabled()) {
            query = "SHOW COLUMNS FROM " + tableName;
        } else {
            query = "PRAGMA table_info(" + tableName + ")";
        }

        try (ResultSet resultSet =
                     this.getDatabaseConnector().sendQuery(query, true)) {
            while (resultSet != null && resultSet.next()) {
                // If there is column called 'weapon', we don't have to patch anymore.
                if (resultSet.getString(this.plugin.getConfigHandler().isMySQLEnabled() ? 1 : 2).equalsIgnoreCase(
                        "weapon")) {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }

        return true;
    }

    @Override
    public boolean applySQLiteChanges() {
        String tableName = this.getDatabaseConnector().getTable(PlayerStat.KILLS_MOBS).getTableName();
        String tempName = tableName + "_temp";

        // Add new column with default value
        // Move old table to new temp table ->
        // Drop old table ->
        // Create new table with weapon column and unique constraint ->
        // Move all data from temp table to new table ->
        // Remove temp table ->
        // Done! :-)
        List<String> queries = Arrays.asList("ALTER TABLE " + tableName + " ADD COLUMN weapon TEXT DEFAULT('HAND') " +
                        "NOT NULL;",

                "CREATE TABLE " + tempName + " AS SELECT * FROM " + tableName + ";", "DROP TABLE " + tableName + ";",
                "CREATE TABLE " + tableName
                        + " (id INTEGER PRIMARY KEY NOT NULL, uuid TEXT NOT NULL, value INTEGER NOT NULL, world TEXT " +
                        "NOT NULL,"
                        + "mob TEXT NOT NULL, weapon TEXT DEFAULT ('HAND') NOT NULL, UNIQUE (uuid,world,mob,weapon));",
                "INSERT INTO " + tableName
                        + " (id, uuid, value, world, mob, weapon) SELECT id, uuid, value, world, mob, weapon FROM "
                        + tempName + ";",
                "DROP TABLE " + tempName + ";");

        try {
            this.getDatabaseConnector().sendQueries(queries, false);

            return true;
        } catch (Exception e) {

            this.getStatz().getLogger().warning("Failed to patch SQLite database for patch " + this.getPatchId());
            return false;
        }

    }

}
