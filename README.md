`/lookup <blockstate/item> <id>`

Looks up the Java identifier from a given integer ID.

`/log bedrock serverbound`

Toggles printing incoming Bedrock packets to the console. 
`bedrock` can be shortcut as `b`; `serverbound` can be shortcut as `sb`.

`/log bedrock serverbound filteradd <packets>`

Takes in a list of packets to *not* print to the packet log.
Format: `/log b sb filteradd PlayerAuthInputPacket MovePlayerPacket`

`/log bedrock serverbound filterrm <packets>`

Removes packets from being filtered from the log. Same format as the `filteradd` command.
