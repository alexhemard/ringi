(ns migrations.20140211204553-create-user
  (:require [clojure.java.jdbc :as jdbc]))

(defn up [db]
  (jdbc/db-do-commands
   db

   "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\""

   "CREATE TABLE IF NOT EXISTS users (
      id         uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
      name       character varying(128) UNIQUE NOT NULL,
      email      character varying(2048) UNIQUE NOT NULL,
      password   character varying(2048),
      created_at timestamp DEFAULT now(),
      updated_at timestamp DEFAULT now(),
      last_login timestamp DEFAULT now())"

   "CREATE TABLE IF NOT EXISTS providers (
      id   uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
      name character varying(128) UNIQUE NOT NULL)"

   "CREATE TABLE IF NOT EXISTS providers_users (
      id          uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
      user_id     uuid references users(id) not null,
      provider_id uuid references providers(id) not null,
      uid         varchar(128),
      token       varchar(2048),
      secret      varchar(2048),
      created_at timestamp DEFAULT now(),
      updated_at timestamp DEFAULT now())"

   "CREATE UNIQUE INDEX index_providers_users_on_provider_id_and_uid ON providers_users USING btree (uid, provider_id)"

   "CREATE UNIQUE INDEX index_providers_users_on_provider_id_and_user_id ON providers_users USING btree (user_id, provider_id)"))
