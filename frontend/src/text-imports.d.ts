// `import x from './file.html' with { loader: 'text' }`（Angular のビルドの text ローダー）の型。
// index.html のインラインスクリプトをテストで読むために使う
declare module '*.html' {
  const content: string;
  export default content;
}
