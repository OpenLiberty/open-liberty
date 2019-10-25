import React from 'react';
import {VariableText} from './VariableText';
import {VariableHeight} from './VariableHeight';
import {FixedHeight} from './FixedHeight';
import {InitiallyOpened} from './InitiallyOpened';
import {SpringConfig} from './SpringConfig';
import {Nested} from './Nested';
import {Hooks} from './Hooks';
import {AutoUnmount} from './AutoUnmount';

import {Issue40} from './Issue40';
import {Issue59} from './Issue59';
import {Issue66} from './Issue66';
import {Issue163} from './Issue163';

import {name} from '../../../package.json';


export const App = () => (
  <div className="app">

    <h1>{name}</h1>

    <section className="section">
      <h2>1. Variable text</h2>
      <VariableText />
    </section>

    <section className="section">
      <h2>2. Variable text (initially opened)</h2>
      <VariableText isOpened={true} />
    </section>

    <section className="section">
      <h2>3. Variable height content</h2>
      <VariableHeight />
    </section>

    <section className="section">
      <h2>4. Fixed height content</h2>
      <FixedHeight />
    </section>

    <section className="section">
      <h2>4. Initially opened</h2>
      <InitiallyOpened />
    </section>

    <section className="section">
      <h2>5. Custom spring configuration</h2>
      <SpringConfig />
    </section>

    <section className="section">
      <h2>6. Nested Collapse</h2>
      <Nested />
    </section>

    <section className="section">
      <h2>7. Hooks</h2>
      <Hooks />
    </section>

    <section className="section">
      <h2>8. Auto-unmount when closed</h2>
      <p>closed by default</p>
      <AutoUnmount isOpened={false} />
      <section className="section">
        <p>opened by default</p>
        <AutoUnmount isOpened={true} />
      </section>
    </section>

    <h1>Edge cases from issues</h1>

    <section className="section">
      <h2>
        <a target="_blank" href="https://github.com/nkbt/react-collapse/issues/40">40</a>.
        Re-render nested components
      </h2>
      <Issue40 />
    </section>


    <section className="section">
      <h2>
        <a target="_blank" href="https://github.com/nkbt/react-collapse/issues/59">59</a>.
        Instantly collapses if re-rendered during collapse
      </h2>
      <Issue59 />
    </section>

    <section className="section">
      <h2>
        <a target="_blank" href="https://github.com/nkbt/react-collapse/issues/66">66</a>.
        Unnecessary unmount with keepCollapsedContent
      </h2>
      <p>Opened by default</p>
      <Issue66 isOpened={true} />
      <p>Closed by default</p>
      <Issue66 isOpened={false} />
    </section>

    <section className="section">
      <h2>
        <a target="_blank" href="https://github.com/nkbt/react-collapse/issues/163">163</a>.
        Overflow in collapse
      </h2>
      <Issue163 />
    </section>

  </div>
);
