# Beyond Ruby w/ Effect Systems

This is a sandbox and code for my talk on why I like Scala and Effect Systems.

## Development

This project uses `nix-shell` to make thing nicer and reproducable.

```bash
nix-shell ~/shell.nix
```

## Ruby side

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
