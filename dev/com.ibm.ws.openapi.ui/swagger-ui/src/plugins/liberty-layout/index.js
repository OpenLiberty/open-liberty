import React from "react"

class LibertyLayout extends React.Component {
    render() {
        const { getComponent } = this.props;

        const Container = getComponent("Container")
        const BaseLayout = getComponent("BaseLayout", true)
        const Headerbar = getComponent("Headerbar", true)
        const FooterContainer = getComponent("FooterContainer", true)

        return (
            <Container className='swagger-ui'>
                <Headerbar />
                <BaseLayout />
                <FooterContainer />
            </Container>
        )
    }
}

export default () => {
    return {
        components: {
            LibertyLayout
        }
    }
}