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

CREATE OR REPLACE VIEW "ViewGroupMembers" AS 
 SELECT groups.id, membership.alias, membership.set_by
   FROM groups
   JOIN membership ON membership.group_id = groups.id;

CREATE OR REPLACE VIEW "ViewMembershipAggregates" AS 
 SELECT membership.group_id AS id, min(membership.modified) AS oldest, max(membership.modified) AS newest, count(*) AS member_count
   FROM membership
  GROUP BY membership.group_id;

CREATE OR REPLACE VIEW "ViewGroupsWithAggregateInfo" AS 
 SELECT groups.id, groups.label, groups.is_public, groups.is_self_serve, groups.owner, groups.created, groups.modified, "ViewMembershipAggregates".oldest, "ViewMembershipAggregates".newest, "ViewMembershipAggregates".member_count
   FROM groups
   JOIN "ViewMembershipAggregates" ON "ViewMembershipAggregates".id = groups.id;

