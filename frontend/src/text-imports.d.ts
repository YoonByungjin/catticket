// `import x from './file.html' with { loader: 'text' }`（Angular のビルドの text ローダー）の型。
// index.html のインラインスクリプトをテストで読むために使う
declare module '*.html' {
  const content: string;
  export default content;
}

// SCSS の値（表示の切り替えの境目）を TypeScript の定数と比べるテストで使う
declare module '*.scss' {
  const content: string;
  export default content;
}
