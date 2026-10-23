# dynlair-core
The core plugin for Dynlair, a Paper plugin to automatically generate user-facing documentation of your systems.

This core plugin offers a registry for documentation sources, and an interface to be used by hook plugins, which implement per-plugin support.

## Commands
 - `/dynlair sources` list registered sources
 - `/dynlair export [source...]` export all, or optionally, only named sources

## Hooks
Dynlair hooks all depend on the core plugin, and the plugin they provide documentation generation for. 
DocSource#export() runs on the main thread and returns a JsonObject.
Any process to collect and transform data into the documentation spec is the hook's own domain, and shares no code with this API.
