To-Do List
==========

- change the `edit` command to `editor`
- use the list of classes to support *all* Scala classes,
  not just immutable collections
    - this is started. need to handle the case of multiple matches,
        such as when the user searches for "Map".
- the `doc` result should have methods like head, tail, take
    - this desire makes me think the code should return a Seq 
      instead of a String

Longer term
-----------
- need something like a `more` and/or `limit` command for long output
- add a `methods` command that lists all of the methods for a class
- add a command to jump to source on Github

Done
----
- add a `help` command or something like that

