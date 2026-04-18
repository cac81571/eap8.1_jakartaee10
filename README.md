# EAP 8.1 で Jakarta EE 10 を動かす Docker プロジェクト

このプロジェクトは、Jakarta EE 10 のサンプル WAR をビルドし、JBoss EAP 8.1 の3ノード構成を HAProxy 配下で実行する最小構成です。
以下の Red Hat ブログ記事の流れに合わせて、EAP builder/runtime のマルチステージで構成しています。

https://rheb.hatenablog.com/entry/2024/06/28/162306

## 構成の整理（サービスと役割）

| コンポーネント | 役割 |
|----------------|------|
| **eap1 / eap2 / eap3** | アプリケーション（`ROOT.war`）。`JGROUPS_*` でクラスタ検出（DNS_PING）。 |
| **haproxy** | HTTP の負荷分散（`eap1` / `eap2` / `eap3`）。設定は `haproxy/haproxy.cfg`。 |
| **datagrid**（Infinispan Server コンテナ） | （任意）検証用途の外部 Infinispan。現状のアプリ本体では必須ではありません。 |

### HTTP セッション共有と「Data Grid」コンテナの違い

- **ブラウザの HTTP セッション（JSF の `@SessionScoped` など）のレプリケーション**は、Red Hat Data Grid 製品サーバに載せ替えているわけではなく、通常どおり **EAP 内蔵の Infinispan（`infinispan` サブシステム）＋クラスタ時は JGroups** が担当します。
- アプリ側では `src/main/webapp/WEB-INF/web.xml` の **`<distributable/>`** でクラスタ上のセッション複製の対象になっています。
- `docker-compose.yml` の **datagrid サービス**は、上記とは別の外部 Infinispan です。セッションストアの代替ではありません。

### ログ（どこで有効か）

- **どの EAP ノードで処理したか**  
  ノード名は `jboss.node.name`（`JAVA_OPTS_APPEND` の `-Djboss.node.name=...`）が付いていればそれを優先します。
- **HAProxy がどのバックエンドに振ったか**  
  `haproxy/haproxy.cfg` の **`defaults`** で `log global` と `log-format` を指定しており、コンテナの **stdout** にアクセスログが出ます。
- **HAProxy のログを止めたい場合**  
  同じく **`haproxy/haproxy.cfg`** で `defaults` の `log global` と `log-format` をコメントアウトし、代わりに `no log` を指定する（`global` の `log stdout ...` も不要ならコメントアウト）。

### EAP 側のセッション／Infinispan 設定を CLI で確認したい場合

HTTP セッション用キャッシュやクラスタは **EAP のサーバ設定**（通常は `$JBOSS_HOME/standalone/configuration/` 以下の `standalone-ha.xml` 等）にあります。このリポジトリでは Galleon の **`cloud-default-config`** でプロビジョニングしており、**カスタム XML は同梱していません**。

起動中のサーバに接続する例:

```bash
$JBOSS_HOME/bin/jboss-cli.sh -c
/subsystem=infinispan:read-resource(recursive=true)
/subsystem=jgroups:read-resource(recursive=true)
/subsystem=undertow:read-resource(recursive=true)
```

オフラインで特定の設定ファイルを編集する例:

```bash
$JBOSS_HOME/bin/jboss-cli.sh
embed-server --server-config=standalone-ha.xml
# 必要な /subsystem=... の操作
stop-embedded-server
```

変更する場合は、**実際に EAP が読み込んでいる設定ファイル名**と、**HA クラスタ用プロファイル**が前提になる点に注意してください。

## 前提

- Docker / Docker Compose が利用可能
- Red Hat コンテナレジストリへログイン可能
  - `registry.redhat.io/jboss-eap-8/eap81-openjdk21-builder-openshift-rhel9`
  - `registry.redhat.io/jboss-eap-8/eap81-openjdk21-runtime-openshift-rhel9`
    を利用するため

## 使い方

1. Red Hat レジストリへログイン

```bash
docker login registry.redhat.io
```

2. WAR を事前ビルド（プリビルド方式）

```bash
mvn -DskipTests package
```

3. イメージをビルドして起動（`eap1`, `eap2`, `eap3`, `datagrid`, `haproxy`）

```bash
docker compose up --build -d
```

4. 動作確認

- トップページ: <http://localhost:8080/>
- HAProxy 統計: <http://localhost:8404/stats>
- `index.xhtml` 上で `Count Up` を押すと、`@SessionScoped` のカウンタが増えます
- 同じブラウザタブではカウンタが維持され、別ブラウザやシークレットウィンドウでは別セッションになります
- フォーム送信後は `faces-redirect=true` で `GET` に遷移する PRG パターン構成です

`docker-compose.yml` は EAP イメージのデフォルト起動をそのまま利用し、`JAVA_OPTS_APPEND` で `jboss.node.name` をノードごとに設定しています。`web.xml` の `<distributable/>` によりセッション複製に対応した構成です。
負荷分散は HAProxy が担当し、バックエンドは `eap1:8080`・`eap2:8080`・`eap3:8080` の3台です。

JSF 画面を利用するため、EAP の Galleon レイヤーに `jsf` を含めています。
`Dockerfile` 変更後は必ず `--no-cache` 付きで再ビルドしてください。
Docker build は `target/ROOT.war` をそのままコピーします。

## 停止

```bash
docker compose down
```
