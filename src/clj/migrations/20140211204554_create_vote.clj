(ns migrations.20140211204554-create-vote
  (:require [clojure.java.jdbc :as jdbc]))

(defn up [db]
  (jdbc/db-do-commands
   db

   "CREATE TABLE IF NOT EXISTS votes (
      id          uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
      author_id   uuid references users(id) NOT NULL,
      title       character varying(1024) NOT NULL,
      description character varying(2048) NOT NULL,
      state       character varying(1024) NOT NULL,
      created_at  timestamp DEFAULT now(),
      updated_at  timestamp DEFAULT now())"

   "CREATE TABLE IF NOT EXISTS choices (
      id         uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
      vote_id    uuid references votes(id) NOT NULL,
      author_id  uuid references users(id) NOT NULL,
      title      character varying(128) NOT NULL,
      created_at timestamp DEFAULT now(),
      updated_at timestamp DEFAULT now())"

   "CREATE TABLE IF NOT EXISTS decisions (
      id         uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
      choice_id  uuid references choices(id) not null,
      author_id  uuid references users(id) not null,
      state      character varying(1024) NOT NULL,
      value      smallint NOT NULL,
      created_at timestamp DEFAULT now(),
      updated_at timestamp DEFAULT now())"

   "CREATE TABLE IF NOT EXISTS comments (
      id         uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
      author_id  uuid references users(id) not null,
      comment    text NOT NULL,
      created_at timestamp DEFAULT now(),
      updated_at timestamp DEFAULT now())"

   "CREATE TABLE IF NOT EXISTS comments_votes (
      vote_id    uuid references votes(id),
      comment_id uuid references comments(id))"

   "CREATE TABLE IF NOT EXISTS comments_choices (
      choice_id  uuid references choices(id),
      comment_id uuid references comments(id))"

   "CREATE UNIQUE INDEX index_decisions_users_on_author_id_and_choice_id ON decisions USING btree (author_id, choice_id) where state = 'active'"

   "CREATE UNIQUE INDEX index_comments_votes_on_vote_id_and_comment_id ON comments_votes USING btree (vote_id, comment_id)"

   "CREATE UNIQUE INDEX index_comments_choices_on_choice_id_and_comment_id ON comments_choices USING btree (choice_id, comment_id)"))
