/*
 *  check usage examples in ./example/typescript-example.tsx
 */

import * as React from 'react';

interface PropConstraints<T> {
  readonly value?: string | number;
  readonly onChange: React.ChangeEventHandler<T>;
}

export type DebounceInputProps<WrappedComponent, WrappedComponentProps> = WrappedComponentProps & {
  readonly element?: string | React.ComponentType<PropConstraints<WrappedComponent>>;
  readonly type?: string;
  readonly onChange: React.ChangeEventHandler<WrappedComponent>;
  readonly onKeyDown?: React.KeyboardEventHandler<WrappedComponent>;
  readonly onBlur?: React.FocusEventHandler<WrappedComponent>;
  readonly value?: string | number;
  readonly placeholder?: string | number;
  readonly minLength?: number;
  readonly debounceTimeout?: number;
  readonly forceNotifyByEnter?: boolean;
  readonly forceNotifyOnBlur?: boolean;
  readonly inputRef?: React.Ref<WrappedComponent>;
};

export declare class DebounceInput<
  WrappedComponent = HTMLInputElement,
  WrappedComponentProps = React.InputHTMLAttributes<HTMLInputElement>
> extends React.PureComponent<DebounceInputProps<WrappedComponent, WrappedComponentProps>> {

}

export type Debounced<
  WrappedComponent,
  WrappedComponentProps
> = React.ComponentType<DebounceInputProps<WrappedComponent, WrappedComponentProps>>;

export type DebounceTextArea = Debounced<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement>
>;
