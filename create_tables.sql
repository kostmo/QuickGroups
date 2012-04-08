CREATE TABLE groups
(
  id serial NOT NULL,
  label character(255),
  is_public boolean,
  is_self_serve boolean,
  "owner" character(30),
  created timestamp without time zone,
  modified timestamp without time zone,
  CONSTRAINT groups_pkey PRIMARY KEY (id)
);

CREATE TABLE membership
(
  alias character(30) NOT NULL,
  group_id integer NOT NULL,
  set_by character(30),
  modified timestamp without time zone DEFAULT now(),
  CONSTRAINT membership_pkey PRIMARY KEY (alias, group_id),
  CONSTRAINT membership_group_id_fkey FOREIGN KEY (group_id)
      REFERENCES groups (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE tags
(
  id serial NOT NULL,
  label character(30),
  creator character(30),
  created timestamp without time zone DEFAULT now(),
  CONSTRAINT tags_pkey PRIMARY KEY (id),
  CONSTRAINT tags_label_key UNIQUE (label)
);

CREATE TABLE tag_associations
(
  group_id integer NOT NULL,
  tag_id integer NOT NULL,
  created timestamp without time zone DEFAULT now(),
  author character(30),
  CONSTRAINT tag_associations_pkey PRIMARY KEY (group_id, tag_id),
  CONSTRAINT tag_associations_group_id_fkey FOREIGN KEY (group_id)
      REFERENCES groups (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT tag_associations_tag_id_fkey FOREIGN KEY (tag_id)
      REFERENCES tags (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE OR REPLACE VIEW "ViewTagHistogram" AS 
 SELECT btrim(min(tags.label)::text) AS label, count(tag_associations.tag_id) AS count
   FROM tags
   LEFT JOIN tag_associations ON tags.id = tag_associations.tag_id
  GROUP BY tags.id;

CREATE OR REPLACE VIEW "ViewGroupMembers" AS 
 SELECT groups.id, membership.alias, membership.set_by
   FROM groups
   JOIN membership ON membership.group_id = groups.id;

CREATE OR REPLACE VIEW "ViewMembershipAggregates" AS 
 SELECT membership.group_id AS id, min(membership.modified) AS oldest, max(membership.modified) AS newest, count(*) AS member_count
   FROM membership
  GROUP BY membership.group_id;

CREATE OR REPLACE VIEW "ViewGroupTags" AS 
 SELECT tag_associations.group_id, btrim(tags.label::text) AS label, tag_associations.tag_id
   FROM tag_associations
   JOIN tags ON tag_associations.tag_id = tags.id
  ORDER BY tag_associations.group_id, tags.label;
  
CREATE OR REPLACE VIEW "ViewTagLists" AS 
 SELECT "ViewGroupTags".group_id, count(*) AS tag_count, array_to_string(array_agg("ViewGroupTags".label), ','::text) AS taglist
   FROM "ViewGroupTags"
  GROUP BY "ViewGroupTags".group_id;

CREATE OR REPLACE VIEW "ViewGroupsWithAggregateInfo" AS 
 SELECT groups.id, groups.label, groups.is_public, groups.is_self_serve, groups.owner, groups.created, groups.modified, "ViewMembershipAggregates".oldest, "ViewMembershipAggregates".newest, COALESCE("ViewMembershipAggregates".member_count, 0::bigint) AS member_count, "ViewTagLists".taglist, "ViewTagLists".tag_count
   FROM groups
   LEFT JOIN "ViewMembershipAggregates" ON "ViewMembershipAggregates".id = groups.id
   JOIN "ViewTagLists" ON "ViewTagLists".group_id = groups.id;