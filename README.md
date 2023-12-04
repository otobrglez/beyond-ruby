# Beyond Ruby w/ Effect Systems

This is a sandbox and code for my talk on why I like Scala and Effect Systems.

## Development

This project uses `nix-shell` to make things more excellent and reproducible.

```bash
nix-shell ~/shell.nix
```

### Environment

The code depends on given environment variables:

- `POSITIONSTACK_API_KEY` - that can be obtained from [positionstack](https://positionstack.com).
- `OPENROUTE_API_KEY` - that can be obtained at [openroute service](https://openrouteservice.org).
- `PROMINFO_QUERY_ENDPOINT` that can be obtained upon request.

## Ruby side of things

Use [stations-cli.rb](./stations-cli.rb) to interact with logic directly.

```bash
./stations-cli.rb near -q "dunajska 5, 1000 ljubljana" -s 10
./stations-cli.rb near_par -q "dunajska 5, 1000 ljubljana" -s 10
./stations-cli.rb near_duration -q "dunajska 5, 1000 ljubljana" -s 10
./stations-cli.rb near_duration_par -q "dunajska 5, 1000 ljubljana" -s 10
```

Spawn the [server.rb](./server.rb) via Puma

```bash
bundle exec puma -p 8776
```

## Author
- [Oto Brglez](https://github.com/otobrglez)

