CREATE TABLE "osmrmhv_cache" (
	"cache_id" TEXT,
	"object_id" BIGINT,
	"data" BYTEA,
	"date" TIMESTAMP,
	PRIMARY KEY ( "cache_id", "object_id" )
);

CREATE TABLE "osmrmhv_cache_version" (
	"cache_id" TEXT,
	"object_id" BIGINT,
	"version" BIGINT,
	"data" BYTEA,
	PRIMARY KEY ( "cache_id", "object_id", "version" )
);
